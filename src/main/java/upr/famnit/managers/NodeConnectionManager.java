package upr.famnit.managers;

import upr.famnit.util.LogLevel;
import upr.famnit.util.Logger;
import upr.famnit.util.Util;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
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

    /**
     * Gets the connected Node socket.
     */
    public Socket getNodeSocket() {
        return nodeSocket;
    }

    public synchronized void proxyRequestToNode(String method, String uri,
                                                Map<String, String> headers, byte[] requestBody,
                                                Socket clientSocket) throws IOException {
        InputStream streamFromNode = nodeSocket.getInputStream();
        OutputStream streamToNode = nodeSocket.getOutputStream();
        InputStream streamFromClient = clientSocket.getInputStream();
        OutputStream streamToClient = clientSocket.getOutputStream();
        DataOutputStream dataToNode = new DataOutputStream(streamToNode);

        // Write content length for the node to read
        dataToNode.writeInt(Util.getTotalLength(method, uri, headers, requestBody));

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

        readAndForwardResponse(streamFromNode, streamToClient);

        Logger.log("Finished forwarding response to client.", LogLevel.network);
    }

    private static void readAndForwardResponse(InputStream streamFromNode, OutputStream streamToClient) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(streamFromNode, StandardCharsets.UTF_8));
        OutputStreamWriter writer = new OutputStreamWriter(streamToClient, StandardCharsets.UTF_8);

        // Read and forward the status line
        String statusLine = reader.readLine();
        if (statusLine == null) {
            throw new IOException("Failed to read status line from node");
        }
        writer.write(statusLine + "\r\n");
        Logger.log("Status Line: " + statusLine, LogLevel.info);

        // Read and forward headers
        Map<String, String> responseHeaders = new HashMap<>();
        String headerLine;
        while ((headerLine = reader.readLine()) != null && !headerLine.isEmpty()) {
            writer.write(headerLine + "\r\n");
            int separatorIndex = headerLine.indexOf(":");
            if (separatorIndex != -1) {
                String headerName = headerLine.substring(0, separatorIndex).trim();
                String headerValue = headerLine.substring(separatorIndex + 1).trim();
                responseHeaders.put(headerName.toLowerCase(), headerValue);
            }
        }
        Logger.log("Response headers: " + responseHeaders);
        writer.write("\r\n");
        writer.flush();

        // Determine how to read the response body
        if (responseHeaders.containsKey("transfer-encoding") && responseHeaders.get("transfer-encoding").equalsIgnoreCase("chunked")) {
            // Read chunked response
            readAndForwardChunkedBody(streamFromNode, streamToClient);
        } else if (responseHeaders.containsKey("content-length")) {
            // Read fixed-length response
            int contentLength = Integer.parseInt(responseHeaders.get("content-length"));
            readAndForwardFixedLengthBody(streamFromNode, streamToClient, contentLength);
        } else {
            // No Content-Length or Transfer-Encoding; read until EOF (use with caution)
            readAndForwardUntilEOF(streamFromNode, streamToClient);
        }
    }

    private static void readAndForwardChunkedBody(InputStream in, OutputStream out) throws IOException {
        Logger.log("Receiving chunked response...", LogLevel.network);
        while (true) {
            // Read the chunk size line
            String chunkSizeLine = Util.readLine(in);
            if (chunkSizeLine == null) {
                throw new IOException("Unexpected end of stream while reading chunk size");
            }
            out.write(chunkSizeLine.getBytes(StandardCharsets.US_ASCII));
            out.write("\r\n".getBytes(StandardCharsets.US_ASCII));
            out.flush();

            int chunkSize;
            try {
                chunkSize = Integer.parseInt(chunkSizeLine.trim(), 16);
            } catch (NumberFormatException e) {
                throw new IOException("Invalid chunk size: " + chunkSizeLine);
            }

            if (chunkSize == 0) {
                // End of chunks
                Logger.log("End of chunked response.", LogLevel.network);
                // Read and forward any trailing headers
                String line;
                while ((line = Util.readLine(in)) != null && !line.isEmpty()) {
                    Logger.log("Trailing header: " + line, LogLevel.network);
                    out.write(line.getBytes(StandardCharsets.US_ASCII));
                    out.write("\r\n".getBytes(StandardCharsets.US_ASCII));
                }
                out.write("\r\n".getBytes(StandardCharsets.US_ASCII));
                out.flush();
                break;
            }

            // Read the chunk data
            byte[] chunkData = new byte[chunkSize];
            int totalBytesRead = 0;
            while (totalBytesRead < chunkSize) {
                int bytesRead = in.read(chunkData, totalBytesRead, chunkSize - totalBytesRead);
                if (bytesRead == -1) {
                    throw new IOException("Unexpected end of stream while reading chunk data");
                }
                totalBytesRead += bytesRead;
            }

            // Write the chunk data to the output
            out.write(chunkData);
            out.write("\r\n".getBytes(StandardCharsets.US_ASCII));
            out.flush();

            // Read the trailing CRLF after the chunk data
            String crlf = Util.readLine(in);
            if (crlf == null || !crlf.isEmpty()) {
                throw new IOException("Expected CRLF after chunk data");
            }
        }
    }

    private static void readAndForwardFixedLengthBody(InputStream in, OutputStream out, int contentLength) throws IOException {
        Logger.log("Receiving fixed length response...", LogLevel.network);
        byte[] buffer = new byte[8192];
        int totalBytesRead = 0;
        int bytesRead;
        while (totalBytesRead < contentLength && (bytesRead = in.read(buffer, 0, Math.min(buffer.length, contentLength - totalBytesRead))) != -1) {
            out.write(buffer, 0, bytesRead);
            out.flush();
            totalBytesRead += bytesRead;
        }
    }

    private static void readAndForwardUntilEOF(InputStream in, OutputStream out) throws IOException {
        Logger.log("Receiving unknown length response...", LogLevel.network);
        byte[] buffer = new byte[8192];
        int bytesRead;
        while ((bytesRead = in.read(buffer)) != -1) {
            out.write(buffer, 0, bytesRead);
            out.flush();
        }
    }
}
