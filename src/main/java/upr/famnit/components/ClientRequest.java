package upr.famnit.components;

import upr.famnit.util.Logger;

import java.io.*;
import java.net.Socket;

import static upr.famnit.util.Config.PROXY_TIMEOUT_MS;

public class ClientRequest {

    private Socket clientSocket;
    private Request request;

    /**
     * Handles client requests by forwarding them to the Node and writing the response
     * back through the clientSocket.
     *
     * @param clientSocket   The client's socket connection.
     */
    public ClientRequest(Socket clientSocket) throws IOException {
        this.clientSocket = clientSocket;
        Logger.log("Parsing client request...");
        // Set a timeout for the client socket
        clientSocket.setSoTimeout(PROXY_TIMEOUT_MS);  // 30 seconds timeout

        InputStream clientInputStream = clientSocket.getInputStream();
        this.request = new Request(clientInputStream);
//        } catch (IOException e) {
//            Logger.log(e.getMessage(), LogLevel.error);
//            Logger.log("Closing connection to client.", LogLevel.error);
//            clientInputStream.close();
//        }
    }

    public Socket getClientSocket() {
        return clientSocket;
    }

    public Request getRequest() {
        return request;
    }
//
//    @Override
//    public void run() {
//        try {
//
//
//            nodeSocket.proxyRequestToNode(request, clientSocket);
//            Logger.log("Request successfully proxied. Closing connection with client.", LogLevel.info);
//            clientSocket.close();
//
//        } catch (SocketTimeoutException e) {
//            Logger.log("Connection timed out: " + e.getMessage(), LogLevel.error);
//        } catch (SocketException e) {
//            Logger.log("Connection reset by peer: " + e.getMessage(), LogLevel.error);
//        } catch (IOException e) {
//            Logger.log("An IOException occurred: " + e.getMessage(), LogLevel.error);
//        } catch (NumberFormatException e) {
//            Logger.log("Invalid Content-Length value in request headers: " + e.getMessage(), LogLevel.warn);
//        } finally {
//            // Ensure the client socket is closed
//            try {
//                clientSocket.close();
//            } catch (IOException e) {
//                Logger.log("Failed to close client socket: " + e.getMessage(), LogLevel.error);
//            }
//        }
//    }
}
