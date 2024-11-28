package upr.famnit.components;

import upr.famnit.util.Logger;
import upr.famnit.util.StreamUtil;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class Connection {

    private final Object socketLock = new Object();
    private final ReentrantLock stateLock = new ReentrantLock();

    private final Socket nodeSocket;
    private final InputStream streamFromNode;
    private final OutputStream streamToNode;
    private boolean connectionOpen;


    public Connection(Socket socket) throws IOException {
        nodeSocket = socket;
        streamFromNode = nodeSocket.getInputStream();
        streamToNode = nodeSocket.getOutputStream();
        connectionOpen = true;
    }

    public boolean isFine() {
        synchronized (stateLock) {
            return connectionOpen && nodeSocket.isConnected();
        }
    }

    public String getInetAddress() {
        synchronized (stateLock) {
            return nodeSocket.getInetAddress().toString();
        }
    }

    public Request waitForRequest() throws IOException {
        synchronized (socketLock) {
            return new Request(nodeSocket);
        }
    }

    public void close() {
        synchronized (socketLock) {
            synchronized (stateLock) {
                try {
                    connectionOpen = false;
                    streamFromNode.close();
                    streamToNode.close();
                    nodeSocket.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    public void send(Request request) throws IOException {
        synchronized (socketLock) {
            StreamUtil.sendRequest(streamToNode, request);
        }
    }

    public void proxyRequestToNode(ClientRequest clientRequest) throws IOException {
        synchronized (socketLock) {
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

    }
}
