package upr.famnit.managers;

import upr.famnit.authentication.KeyUtil;
import upr.famnit.authentication.VerificationType;
import upr.famnit.components.*;
import upr.famnit.util.Logger;
import upr.famnit.util.StreamUtil;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Map;

import static upr.famnit.util.Config.CONNECTION_EXCEPTION_THRESHOLD;

/**
 * Handles the connection from the Node.
 */
public class NodeConnectionManager extends Thread {

    private final Socket nodeSocket;
    private boolean connectionOpen;
    private LocalDateTime lastPing;
    private int connectionExceptionCount;
    private String nodeName;

    public NodeConnectionManager(ServerSocket nodeServerSocket) throws IOException {
        nodeSocket = nodeServerSocket.accept();
        connectionOpen = true;
        lastPing = LocalDateTime.now();
        connectionExceptionCount = 0;
        nodeName = null;
        Logger.log("Worker node connected: " + nodeSocket.getInetAddress(), LogLevel.status);
    }

    @Override
    public void run() {
        Logger.log("Worker thread started.", LogLevel.status);
        try {
            authenticateNode();
        } catch (IOException e) {
            Logger.log("Error authenticating worker node: " + e.getMessage(), LogLevel.error);
        }

        while (connectionOpen && nodeSocket.isConnected()) {
            try {

                Request request = new Request(nodeSocket);
                handleRequest(request);

            } catch (IOException e) {

                connectionExceptionCount += 1;
                Logger.log("Problem receiving request from worker node. Count: " + connectionExceptionCount, LogLevel.error);

                if (connectionExceptionCount >= CONNECTION_EXCEPTION_THRESHOLD) {
                    Logger.log("Too many exceptions. Closing connection.", LogLevel.warn);
                    connectionOpen = false;
                }

                if (!connectionOpen) {
                    try {
                        closeConnection();
                    } catch (IOException ex) {
                        throw new RuntimeException(ex);
                    }
                }
            }
        }
        Logger.log("Worker thread closing.", LogLevel.status);
    }

    private void authenticateNode() throws IOException {
        try {
            nodeName = waitForAuthRequestAndValidate();
            Logger.log("Worker authenticated: " + this.nodeName, LogLevel.status);
        } catch (IOException e) {
            Logger.log("Error authenticating node: " + e.getMessage(), LogLevel.error);
        }

        if (nodeName == null) {
            try {
                closeConnection();
            } catch (IOException e) {
                connectionOpen = false;
            }
            return;
        }

        Thread.currentThread().setName(nodeName);
    }

    private String waitForAuthRequestAndValidate() throws IOException {
        Request request = new Request(nodeSocket);
        if (!request.getProtocol().equals("HIVE") || !request.getMethod().equals("AUTH")) {
            throw new IOException("First message should be authentication");
        }

        if (!KeyUtil.verifyKey(request.getUri(), VerificationType.NodeConnection)) {
            throw new IOException("Not valid authentication key.");
        }

        return KeyUtil.nameKey(request.getUri());
    }

    private void handleRequest(Request request) throws IOException {
        if (request.getProtocol().equals("HIVE")) {
            switch (request.getMethod()) {
                case "POLL" -> handlePollRequest(request);
                case null, default -> handlePing(request);
            }
        }
    }

    private void handlePing(Request request) {
        lastPing = LocalDateTime.now();
    }

    private void handlePollRequest(Request request) throws IOException {
        handlePing(request);

        ClientRequest clientRequest = RequestQue.getTask(request.getUri(), nodeName);
        if (clientRequest == null) {
            StreamUtil.sendRequest(
                nodeSocket.getOutputStream(),
                RequestFactory.EmptyQueResponse()
            );
            return;
        }
        Logger.log("Pulled task.");

        try {
            proxyRequestToNode(clientRequest);
        } catch (IOException e) {
            StreamUtil.sendResponse(
                clientRequest.getClientSocket().getOutputStream(),
                ResponseFactory.BadRequest()
            );
        }
    }

    public synchronized void proxyRequestToNode(ClientRequest clientRequest) throws IOException {
        InputStream streamFromNode = nodeSocket.getInputStream();
        OutputStream streamToNode = nodeSocket.getOutputStream();
        OutputStream streamToClient = clientRequest.getClientSocket().getOutputStream();

        StreamUtil.sendRequest(streamToNode, clientRequest.getRequest());
        Logger.log("Request forwarded to Node.", LogLevel.network);

        // Read the status line
        String statusLine = StreamUtil.readLine(streamFromNode);
        if (statusLine == null || statusLine.isEmpty()) {
            throw new IOException("Failed to read status line from node");
        }
        Logger.log("Status Line: " + statusLine, LogLevel.info);
        streamToClient.write((statusLine + "\r\n").getBytes(StandardCharsets.US_ASCII));

        // Read the response headers
        Map<String, String> responseHeaders = StreamUtil.readHeaders(streamFromNode);
        Logger.log("Response headers: " + responseHeaders);

        // Write headers to the client
        for (Map.Entry<String, String> header : responseHeaders.entrySet()) {
            String headerLine = header.getKey() + ": " + header.getValue();
            streamToClient.write((headerLine + "\r\n").getBytes(StandardCharsets.US_ASCII));
        }
        streamToClient.write("\r\n".getBytes(StandardCharsets.US_ASCII));
        streamToClient.flush();

        // Decide how to read the body
        if (responseHeaders.containsKey("transfer-encoding") &&
                responseHeaders.get("transfer-encoding").equalsIgnoreCase("chunked")) {
            StreamUtil.readAndForwardChunkedBody(streamFromNode, streamToClient);
        } else if (responseHeaders.containsKey("content-length")) {
            int contentLength = Integer.parseInt(responseHeaders.get("content-length"));
            StreamUtil.readAndForwardFixedLengthBody(streamFromNode, streamToClient, contentLength);
        } else {
            StreamUtil.readAndForwardUntilEOF(streamFromNode, streamToClient);
        }

        Logger.log("Finished forwarding response to client.", LogLevel.network);
    }

    public synchronized LocalDateTime getLastPing() {
        return lastPing;
    }

    public synchronized void closeConnection() throws IOException {
        this.connectionOpen = false;
        this.nodeSocket.close();
    }
}
