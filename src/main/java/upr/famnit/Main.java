package upr.famnit;

import upr.famnit.managers.NodeConnectionManager;
import upr.famnit.util.Logger;
import upr.famnit.util.LogLevel;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

import static upr.famnit.util.Config.*;

public class Main {

    public static void main(String[] args) {
        try {
            // Start the Node connection handler
            NodeConnectionManager nodeHandler = new NodeConnectionManager(NODE_CONNECTION_PORT);
            new Thread(() -> {
                try {
                    nodeHandler.acceptConnection();
                } catch (IOException e) {
                    Logger.log("Error accepting connection from Node: " + e.getMessage(), LogLevel.error);
                    e.printStackTrace();
                }
            }).start();

            // Start the proxy server for clients
            ServerSocket serverSocket = new ServerSocket(PROXY_PORT);
            Logger.log("Proxy server is running on port " + PROXY_PORT + "...");

            // Wait for the Node to connect
            while (nodeHandler.getNodeSocket() == null) {
                Thread.sleep(100);
            }

            // Log when Node is connected
            Logger.log("Node connected: " + nodeHandler.getNodeSocket().getRemoteSocketAddress(), LogLevel.network);

            while (true) {
                // Accept incoming client connection
                Socket clientSocket = serverSocket.accept();
                Logger.log("Client connected: " + clientSocket.getRemoteSocketAddress(), LogLevel.network);

                // Handle the request in a new thread, passing the Node socket
                new Thread(() -> handleClientRequest(clientSocket, nodeHandler.getNodeSocket())).start();
            }
        } catch (IOException | InterruptedException e) {
            Logger.log("An error occurred in the main thread: " + e.getMessage(), LogLevel.error);
            e.printStackTrace();
        }
    }

    /**
     * Handles client requests by forwarding them to the Node and returning the response.
     *
     * @param clientSocket   The client's socket connection.
     * @param nodeSocket The Node's socket connection.
     */
    private static void handleClientRequest(Socket clientSocket, Socket nodeSocket) {
        try {
            // Set a timeout for the client socket
            clientSocket.setSoTimeout(PROXY_TIMEOUT_MS);  // 30 seconds timeout

            InputStream clientInputStream = clientSocket.getInputStream();

            // Read the client's request line
            String requestLine = readLine(clientInputStream);
            if (requestLine == null || requestLine.isEmpty()) {
                Logger.log("Received empty request from client. Closing connection.", LogLevel.warn);
                clientSocket.close();
                return;
            }

            // Parse the request line
            String[] requestParts = requestLine.split(" ");
            String method = requestParts[0];
            String uri = requestParts[1];
            Logger.log("Request Line: " + requestLine, LogLevel.info);

            // Read the request headers
            Map<String, String> requestHeaders = readRequestHeaders(clientInputStream);
            Logger.log("Request Headers: " + requestHeaders.toString(), LogLevel.info);
            int contentLength = contentLength = Integer.parseInt(requestHeaders.get("Content-Length"));

            // Read the request body if present
            byte[] requestBody = null;
            if (("POST".equalsIgnoreCase(method) || "PUT".equalsIgnoreCase(method)) && contentLength > 0) {
                requestBody = readRequestBody(clientInputStream, contentLength);
                Logger.log("Read request body of length " + contentLength + " bytes.", LogLevel.info);
            }

            // Forward the request to the Node and get the response
            proxyRequestToNode(method, uri, requestHeaders, requestBody, nodeSocket, clientSocket);

            // Close the client connection
            Logger.log("Closing connection with client.", LogLevel.info);
            clientSocket.close();

        } catch (SocketTimeoutException e) {
            Logger.log("Connection timed out: " + e.getMessage(), LogLevel.error);
        } catch (SocketException e) {
            Logger.log("Connection reset by peer: " + e.getMessage(), LogLevel.error);
        } catch (IOException e) {
            Logger.log("An IOException occurred: " + e.getMessage(), LogLevel.error);
        } catch (NumberFormatException e) {
            Logger.log("Invalid Content-Length value in request headers: " + e.getMessage(), LogLevel.warn);
        } finally {
            // Ensure the client socket is closed
            try {
                clientSocket.close();
            } catch (IOException e) {
                Logger.log("Failed to close client socket: " + e.getMessage(), LogLevel.error);
            }
        }
    }

    /**
     * Reads the request headers from the client input stream.
     *
     * @param inputStream The client's input stream.
     * @return A map of header names and values.
     * @throws IOException If an I/O error occurs.
     */
    private static Map<String, String> readRequestHeaders(InputStream inputStream) throws IOException {
        Map<String, String> headers = new LinkedHashMap<>();
        String headerLine;
        while ((headerLine = readLine(inputStream)) != null && !headerLine.isEmpty()) {
            int separatorIndex = headerLine.indexOf(":");
            if (separatorIndex != -1) {
                String headerName = headerLine.substring(0, separatorIndex).trim();
                String headerValue = headerLine.substring(separatorIndex + 1).trim();
                headers.put(headerName, headerValue);
            }
        }
        return headers;
    }

    /**
     * Reads the request body from the client input stream.
     *
     * @param inputStream   The client's input stream.
     * @param contentLength The length of the content to read.
     * @return A byte array containing the request body.
     * @throws IOException If an I/O error occurs.
     */
    private static byte[] readRequestBody(InputStream inputStream, int contentLength) throws IOException {
        byte[] requestBody = new byte[contentLength];
        int totalBytesRead = 0;
        while (totalBytesRead < contentLength) {
            int bytesRead = inputStream.read(requestBody, totalBytesRead, contentLength - totalBytesRead);
            if (bytesRead == -1) {
                // Client closed the connection
                throw new IOException("Client closed the connection while reading the request body.");
            }
            totalBytesRead += bytesRead;
        }
        return requestBody;
    }

    /**
     * Forwards the request to the Node and retrieves the response.
     *
     * @param method         The HTTP method.
     * @param uri            The request URI.
     * @param headers        The request headers.
     * @param requestBody    The request body.
     * @param nodeSocket The Node's socket connection.
     * @param clientSocket   The client's socket connection.
     * @throws IOException If an I/O error occurs.
     */
    private static void proxyRequestToNode(String method, String uri,
                                                 Map<String, String> headers, byte[] requestBody,
                                                 Socket nodeSocket, Socket clientSocket) throws IOException {
        // Get streams
        OutputStream nodeOutputStream = nodeSocket.getOutputStream();
        InputStream nodeInputStream = nodeSocket.getInputStream();
        OutputStream clientOutputStream = clientSocket.getOutputStream();

        // Serialize and send the request to the Node
        ByteArrayOutputStream requestStream = new ByteArrayOutputStream();

        // Write request line
        requestStream.write((method + " " + uri + " HTTP/1.1\r\n").getBytes(StandardCharsets.UTF_8));

        // Write headers
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            requestStream.write((entry.getKey() + ": " + entry.getValue() + "\r\n").getBytes(StandardCharsets.UTF_8));
        }
        requestStream.write("\r\n".getBytes(StandardCharsets.UTF_8)); // End of headers

        // Write body if present
        if (requestBody != null && requestBody.length > 0) {
            requestStream.write(requestBody);
        }

        // Send the length and the request
        DataOutputStream nodeDataOut = new DataOutputStream(nodeOutputStream);
        byte[] requestBytes = requestStream.toByteArray();
        nodeDataOut.writeInt(requestBytes.length);
        nodeDataOut.write(requestBytes);
        nodeDataOut.flush();

        Logger.log("Request forwarded to Node.", LogLevel.network);

        // Read from nodeInputStream and write to clientOutputStream
        byte[] buffer = new byte[MESSAGE_CHUNK_BUFFER_SIZE];
        int bytesRead;
        while ((bytesRead = nodeInputStream.read(buffer)) != -1) {
            clientOutputStream.write(buffer, 0, bytesRead);
            clientOutputStream.flush();
        }

        Logger.log("Finished forwarding response to client.", LogLevel.network);
    }

    /**
     * Reads a line from the input stream.
     *
     * @param in The input stream.
     * @return The line read, or null if end of stream.
     * @throws IOException If an I/O error occurs.
     */
    private static String readLine(InputStream in) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int c;

        while ((c = in.read()) != -1) {
            if (c == '\r') {
                in.mark(1);
                int nextChar = in.read();
                if (nextChar != '\n' && nextChar != -1) {
                    in.reset();
                }
                break;
            } else if (c == '\n') {
                break;
            } else {
                buffer.write(c);
            }
        }

        if (c == -1 && buffer.size() == 0) {
            return null;
        }

        return buffer.toString(StandardCharsets.UTF_8.name());
    }
}
