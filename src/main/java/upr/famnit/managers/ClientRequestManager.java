package upr.famnit.managers;

import upr.famnit.util.LogLevel;
import upr.famnit.util.Logger;
import upr.famnit.util.Util;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static upr.famnit.util.Config.PROXY_TIMEOUT_MS;

public class ClientRequestManager implements Runnable {

    private Socket clientSocket;
    private NodeConnectionManager nodeSocket;

    /**
     * Handles client requests by forwarding them to the Node and returning the response.
     *
     * @param clientSocket   The client's socket connection.
     * @param nodeSocket The Node's socket connection.
     */
    public ClientRequestManager(Socket clientSocket, NodeConnectionManager nodeSocket) {
        this.clientSocket = clientSocket;
        this.nodeSocket = nodeSocket;
    }

    @Override
    public void run() {
        try {
            Logger.log("Handling client");
            // Set a timeout for the client socket
            clientSocket.setSoTimeout(PROXY_TIMEOUT_MS);  // 30 seconds timeout

            // Read the client's request line
            InputStream clientInputStream = clientSocket.getInputStream();
            String requestLine = Util.readLine(clientInputStream);
            if (requestLine == null || requestLine.isEmpty()) {
                Logger.log("Received empty request from client. Closing connection.", LogLevel.error);
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
            Logger.log("Request Headers: " + requestHeaders, LogLevel.info);
            int contentLength = Integer.parseInt(requestHeaders.get("Content-Length"));

            // Read the request body if present
            byte[] requestBody = null;
            if (("POST".equalsIgnoreCase(method) || "PUT".equalsIgnoreCase(method)) && contentLength > 0) {
                requestBody = readRequestBody(clientInputStream, contentLength);
                Logger.log("Read request body of length " + contentLength + " bytes.", LogLevel.info);
            }

            // Forward the request to the Node and get the response
            nodeSocket.proxyRequestToNode(method, uri, requestHeaders, requestBody, clientSocket);

            // Close the client connection
            Logger.log("Request successfully proxied. Closing connection with client.", LogLevel.info);
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
        while ((headerLine = Util.readLine(inputStream)) != null && !headerLine.isEmpty()) {
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

//    /**
//     * Forwards the request to the Node and retrieves the response.
//     *
//     * @param method         The HTTP method.
//     * @param uri            The request URI.
//     * @param headers        The request headers.
//     * @param requestBody    The request body.
//     * @param nodeSocket     The Node's socket connection.
//     * @param clientSocket   The client's socket connection.
//     * @throws IOException   If an I/O error occurs.
//     */
//    private static void proxyRequestToNode(String method, String uri,
//                                           Map<String, String> headers, byte[] requestBody,
//                                           Socket nodeSocket, Socket clientSocket) throws IOException {
//        InputStream streamFromNode = nodeSocket.getInputStream();
//        OutputStream streamToNode = nodeSocket.getOutputStream();
//        InputStream streamFromClient = clientSocket.getInputStream();
//        OutputStream streamToClient = clientSocket.getOutputStream();
//        DataOutputStream dataToNode = new DataOutputStream(streamToNode);
//
//        // Write content length for the node to read
//        dataToNode.writeInt(Util.getTotalLength(method, uri, headers, requestBody));
//
//        // Write the request to node
//        dataToNode.write((method + " " + uri + " HTTP/1.1\r\n").getBytes(StandardCharsets.UTF_8));
//        for (Map.Entry<String, String> entry : headers.entrySet()) {
//            dataToNode.write((entry.getKey() + ": " + entry.getValue() + "\r\n").getBytes(StandardCharsets.UTF_8));
//        }
//        dataToNode.write("\r\n".getBytes(StandardCharsets.UTF_8));  // End of headers
//        if (requestBody != null && requestBody.length > 0) {
//            dataToNode.write(requestBody);
//        }
//        dataToNode.flush();
//        Logger.log("Request forwarded to Node.", LogLevel.network);
//
//        readAndForwardResponse(streamFromNode, streamToClient);
//
//        Logger.log("Finished forwarding response to client.", LogLevel.network);
//    }





}
