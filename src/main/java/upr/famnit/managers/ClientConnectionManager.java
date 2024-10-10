package upr.famnit.managers;

import upr.famnit.components.*;
import upr.famnit.util.Logger;
import upr.famnit.util.StreamUtil;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class ClientConnectionManager extends Thread {

    private final Socket clientSocket;

    public ClientConnectionManager(ServerSocket clientServer) throws IOException {
        this.clientSocket = clientServer.accept();
    }

    @Override
    public void run() {
        Logger.log("Client connected: " + clientSocket.getRemoteSocketAddress(), LogLevel.network);
        ClientRequest cr = null;
        try {
            cr = new ClientRequest(clientSocket);
        } catch (IOException e) {
            Logger.log("Failed reading client request: " + e.getMessage(), LogLevel.error);
        }

        if (cr == null) {
            Logger.log("Not adding the request to the que. Communication stopped. ", LogLevel.error);
            return;
        }

        if (!RequestQue.addTask(cr)) {
            Logger.log("Closing request due to invalid structure.", LogLevel.error);
            Response failedResponse = ResponseFactory.MethodNotAllowed();
            try {
                StreamUtil.sendResponse(cr.getClientSocket().getOutputStream(), failedResponse);
            } catch (IOException e) {
                Logger.log("Unable to respond to the client: " + e.getMessage(), LogLevel.error);
            }
            try {
                cr.getClientSocket().close();
            } catch (IOException e) {
                Logger.log("Unable to close connection to the client: " + e.getMessage(), LogLevel.error);
            }
        }
    }
}
