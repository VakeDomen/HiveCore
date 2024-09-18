package upr.famnit;

//import upr.famnit.managers.NetworkManager;
import upr.famnit.managers.NodeConnectionManager;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

public class Main {
    private static final int PROXY_PORT = 6666;      // Port for client connections
    private static final int RUST_NODE_PORT = 7777;  // Port for Rust Node connection

    public static void main(String[] args) {
        try {
            // Start the Rust Node connection handler
            NodeConnectionManager rustNodeHandler = new NodeConnectionManager(RUST_NODE_PORT);
            new Thread(() -> {
                try {
                    rustNodeHandler.acceptConnection();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }).start();

            // Start the proxy server for clients
            ServerSocket serverSocket = new ServerSocket(PROXY_PORT);
            System.out.println("Proxy server is running on port " + PROXY_PORT + "...");

            // Wait for the Rust Node to connect
            while (rustNodeHandler.getRustNodeSocket() == null) {
                Thread.sleep(100);
            }

            while (true) {
                // Accept incoming client connection
                Socket clientSocket = serverSocket.accept();
                // Handle the request in a new thread, passing the Rust Node socket
                new Thread(() -> handleClientRequest(clientSocket, rustNodeHandler.getRustNodeSocket())).start();
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Handles client requests by forwarding them to the Rust Node and returning the response.
     *
     * @param clientSocket   The client's socket connection.
     * @param rustNodeSocket The Rust Node's socket connection.
     */
    private static void handleClientRequest(Socket clientSocket, Socket rustNodeSocket) {
        try {
            // Set a timeout for the client socket
            clientSocket.setSoTimeout(30000);  // 30 seconds timeout

            // Input and output streams for client communication
            InputStream clientInputStream = clientSocket.getInputStream();
            OutputStream clientOutputStream = clientSocket.getOutputStream();

            // Read the client's request line
            String requestLine = readLine(clientInputStream);
            if (requestLine == null || requestLine.isEmpty()) {
                System.out.println("Received empty request. Closing connection.");
                clientSocket.close();
                return;
            }

            // Parse the request line
            String[] requestParts = requestLine.split(" ");
            String method = requestParts[0];
            String uri = requestParts[1];
            System.out.println("Request Line: " + requestLine);

            // Read the request headers
            Map<String, String> requestHeaders = readRequestHeaders(clientInputStream);
            int contentLength = getContentLength(requestHeaders);

            // Read the request body if present
            byte[] requestBody = null;
            if (("POST".equalsIgnoreCase(method) || "PUT".equalsIgnoreCase(method)) && contentLength > 0) {
                requestBody = readRequestBody(clientInputStream, contentLength);
            }

            // Forward the request to the Rust Node and get the response
            forwardRequestToRustNode(method, uri, requestHeaders, requestBody, rustNodeSocket, clientSocket);

            // Close the client connection
            clientSocket.close();
            System.out.println("Closing connection with client.");

        } catch (SocketTimeoutException e) {
            System.err.println("Connection timed out: " + e.getMessage());
        } catch (SocketException e) {
            System.err.println("Connection reset by peer: " + e.getMessage());
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            // Ensure the client socket is closed
            try {
                clientSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
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
     * Retrieves the Content-Length from the request headers.
     *
     * @param headers The request headers.
     * @return The content length, or 0 if not specified.
     */
    private static int getContentLength(Map<String, String> headers) {
        int contentLength = 0;
        if (headers.containsKey("Content-Length")) {
            contentLength = Integer.parseInt(headers.get("Content-Length"));
        }
        return contentLength;
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
                break;
            }
            totalBytesRead += bytesRead;
        }
        return requestBody;
    }

    /**
     * Forwards the request to the Rust Node and retrieves the response.
     *
     * @param method        The HTTP method.
     * @param uri           The request URI.
     * @param headers       The request headers.
     * @param requestBody   The request body.
     * @param rustNodeSocket The Rust Node's socket connection.
     * @return The response bytes from the Rust Node.
     * @throws IOException If an I/O error occurs.
     */
    private static void forwardRequestToRustNode(String method, String uri,
                                                 Map<String, String> headers, byte[] requestBody,
                                                 Socket rustNodeSocket, Socket clientSocket) throws IOException {
        // Get streams
        OutputStream rustOutputStream = rustNodeSocket.getOutputStream();
        InputStream rustInputStream = rustNodeSocket.getInputStream();
        OutputStream clientOutputStream = clientSocket.getOutputStream();

        // Serialize and send the request to the Rust Node
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
        DataOutputStream rustDataOut = new DataOutputStream(rustOutputStream);
        byte[] requestBytes = requestStream.toByteArray();
        rustDataOut.writeInt(requestBytes.length);
        rustDataOut.write(requestBytes);
        rustDataOut.flush();

        System.out.println("Sent!");

        // Read from rustInputStream and write to clientOutputStream
        byte[] buffer = new byte[8192];
        int bytesRead;
        while ((bytesRead = rustInputStream.read(buffer)) != -1) {
            clientOutputStream.write(buffer, 0, bytesRead);
            System.out.println("READ: " + new String(buffer, 0, bytesRead, StandardCharsets.UTF_8));
            clientOutputStream.flush();
        }

        System.out.println("END");
        // Close the client output stream after forwarding is complete
//        clientOutputStream.close();
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