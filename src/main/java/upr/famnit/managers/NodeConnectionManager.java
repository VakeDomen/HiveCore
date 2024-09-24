package upr.famnit.managers;

import upr.famnit.components.*;
import upr.famnit.util.Logger;
import upr.famnit.util.StreamUtil;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Handles the connection from the Node.
 */
public class NodeConnectionManager extends Thread {

    private final Socket nodeSocket;
    private boolean connectionOpen;

    public NodeConnectionManager(ServerSocket nodeServerSocket) throws IOException {
        System.out.println("Waiting for worker node to connect...");
        nodeSocket = nodeServerSocket.accept();
        connectionOpen = true;
        System.out.println("Worker node connected: " + nodeSocket.getInetAddress());
    }

    @Override
    public void run() {
        while (connectionOpen) {
            try {
                Request request = new Request(nodeSocket.getInputStream());
                ClientRequest clientRequest = RequestQue.getTask("mistral-nemo");
                if (clientRequest == null) {
                    Request emptyQueResponse = RequestFactory.EmptyQueResponse();
                    StreamUtil.sendRequest(nodeSocket.getOutputStream(), emptyQueResponse);
                    continue;
                }
                proxyRequestToNode(clientRequest);

            } catch (IOException e) {
                throw new RuntimeException(e);
            }
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
        if (responseHeaders.containsKey("Transfer-Encoding") &&
                responseHeaders.get("Transfer-Encoding").equalsIgnoreCase("chunked")) {
            StreamUtil.readAndForwardChunkedBody(streamFromNode, streamToClient);
        } else if (responseHeaders.containsKey("Content-Length")) {
            int contentLength = Integer.parseInt(responseHeaders.get("content-length"));
            StreamUtil.readAndForwardFixedLengthBody(streamFromNode, streamToClient, contentLength);
        } else {
            StreamUtil.readAndForwardUntilEOF(streamFromNode, streamToClient);
        }

        Logger.log("Finished forwarding response to client.", LogLevel.network);
    }

    public synchronized void proxyRequestToNode(String protocol, String method, String uri,
                                                Map<String, String> headers, byte[] requestBody,
                                                Socket clientSocket) throws IOException {

    }
}
