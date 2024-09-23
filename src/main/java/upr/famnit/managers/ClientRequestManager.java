package upr.famnit.managers;

import upr.famnit.util.LogLevel;
import upr.famnit.util.Logger;
import upr.famnit.util.StreamUtil;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.Map;

import static upr.famnit.util.Config.PROXY_TIMEOUT_MS;

public class ClientRequestManager implements Runnable {

    private Socket clientSocket;
    private NodeConnectionManager nodeSocket;

    /**
     * Handles client requests by forwarding them to the Node and writing the response
     * back through the clientSocket.
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
            Logger.log("Parsing client request...");
            // Set a timeout for the client socket
            clientSocket.setSoTimeout(PROXY_TIMEOUT_MS);  // 30 seconds timeout

            // Read the client's request line
            InputStream clientInputStream = clientSocket.getInputStream();
            String requestLine = StreamUtil.readLine(clientInputStream);
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
            Map<String, String> requestHeaders = StreamUtil.readHeaders(clientInputStream);
            Logger.log("Request Headers: " + requestHeaders, LogLevel.info);
            int contentLength = Integer.parseInt(requestHeaders.get("Content-Length"));

            // Read the request body if present
            byte[] requestBody = null;
            if (("POST".equalsIgnoreCase(method) || "PUT".equalsIgnoreCase(method)) && contentLength > 0) {
                requestBody = StreamUtil.readRequestBody(clientInputStream, contentLength);
                Logger.log("Read request body of length " + contentLength + " bytes.", LogLevel.info);
            }


            nodeSocket.proxyRequestToNode(method, uri, requestHeaders, requestBody, clientSocket);
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
}
