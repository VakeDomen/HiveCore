package upr.famnit.managers;

import upr.famnit.util.LogLevel;
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
public class NodeConnectionManager {

    private Socket nodeSocket;

    /**
     * Accepts a connection from the Node.
     */
    public void acceptConnection(ServerSocket nodeServerSocket) throws IOException {
        System.out.println("Waiting for worker node to connect...");
        nodeSocket = nodeServerSocket.accept();
        System.out.println("Worker node connected: " + nodeSocket.getInetAddress());
    }

    public synchronized void proxyRequestToNode(String method, String uri,
                                                Map<String, String> headers, byte[] requestBody,
                                                Socket clientSocket) throws IOException {
        InputStream streamFromNode = nodeSocket.getInputStream();
        OutputStream streamToNode = nodeSocket.getOutputStream();
        OutputStream streamToClient = clientSocket.getOutputStream();
        DataOutputStream dataToNode = new DataOutputStream(streamToNode);

        // Write content length for the node to read
        dataToNode.writeInt(StreamUtil.getTotalLength(method, uri, headers, requestBody));

        // Write the request to node
        dataToNode.write((method + " " + uri + " HTTP/1.1\r\n").getBytes(StandardCharsets.UTF_8));
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            dataToNode.write((entry.getKey() + ": " + entry.getValue() + "\r\n").getBytes(StandardCharsets.UTF_8));
        }
        dataToNode.write("\r\n".getBytes(StandardCharsets.UTF_8));  // End of headers
        if (requestBody != null && requestBody.length > 0) {
            dataToNode.write(requestBody);
        }
        dataToNode.flush();
        Logger.log("Request forwarded to Node.", LogLevel.network);

        // readAndForwardResponse(streamFromNode, streamToClient);
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
}
