package upr.famnit.managers;

import upr.famnit.util.LogLevel;
import upr.famnit.util.Logger;

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
    private Socket nodeSocket;

    /**
     * Handles client requests by forwarding them to the Node and returning the response.
     *
     * @param clientSocket   The client's socket connection.
     * @param nodeSocket The Node's socket connection.
     */
    public ClientRequestManager(Socket clientSocket, Socket nodeSocket) {
        this.clientSocket = clientSocket;
        this.nodeSocket = nodeSocket;
    }

    @Override
    public void run() {
        try {
            // Set a timeout for the client socket
            clientSocket.setSoTimeout(PROXY_TIMEOUT_MS);  // 30 seconds timeout

            // Read the client's request line
            InputStream clientInputStream = clientSocket.getInputStream();
            String requestLine = readLine(clientInputStream);
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
            proxyRequestToNode(method, uri, requestHeaders, requestBody, nodeSocket, clientSocket);

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
     * @param nodeSocket     The Node's socket connection.
     * @param clientSocket   The client's socket connection.
     * @throws IOException   If an I/O error occurs.
     */
    private static void proxyRequestToNode(String method, String uri,
                                           Map<String, String> headers, byte[] requestBody,
                                           Socket nodeSocket, Socket clientSocket) throws IOException {
        InputStream streamFromNode = nodeSocket.getInputStream();
        OutputStream streamToNode = nodeSocket.getOutputStream();
        InputStream streamFromClient = clientSocket.getInputStream();
        OutputStream streamToClient = clientSocket.getOutputStream();
        DataOutputStream dataToNode = new DataOutputStream(streamToNode);

        // Write content length for the node to read
        dataToNode.writeInt(getTotalLength(method, uri, headers, requestBody));

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
            String chunkSizeLine = readLine(in);
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
                while ((line = readLine(in)) != null && !line.isEmpty()) {
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
            String crlf = readLine(in);
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

    /**
     * Calculates the byte size of the request that would be created with the civen data
     *
     * @param method        The HTTP method
     * @param uri           The request URI
     * @param headers       The request headers
     * @param requestBody   The request body in bytes
     * @return              The number of content-length bytes
     */
    private static int getTotalLength(String method, String uri, Map<String, String> headers, byte[] requestBody) {
        // Calculate the total length of the request (headers + body)
        int totalLength = (method + " " + uri + " HTTP/1.1\r\n").getBytes(StandardCharsets.UTF_8).length;
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            totalLength += (entry.getKey() + ": " + entry.getValue() + "\r\n").getBytes(StandardCharsets.UTF_8).length;
        }
        totalLength += "\r\n".getBytes(StandardCharsets.UTF_8).length;  // End of headers

        if (requestBody != null && requestBody.length > 0) {
            totalLength += requestBody.length;  // Add the length of the request body
        }
        return totalLength;
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
