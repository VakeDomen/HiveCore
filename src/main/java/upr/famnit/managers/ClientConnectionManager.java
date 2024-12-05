package upr.famnit.managers;

import upr.famnit.components.*;
import upr.famnit.util.Logger;
import upr.famnit.util.StreamUtil;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class ClientConnectionManager implements Runnable {

    private final Socket clientSocket;

    public ClientConnectionManager(ServerSocket clientServer) throws IOException {
        this.clientSocket = clientServer.accept();
    }

    @Override
    public void run() {
        Logger.network("Client connected: " + clientSocket.getRemoteSocketAddress());
        ClientRequest cr = null;
        try {
            cr = new ClientRequest(clientSocket);
        } catch (IOException e) {
            Logger.error("Failed reading client request: " + e.getMessage());
        }

        if (cr == null) {
            Logger.error("Not adding the request to the que. Stopping communication. ");
            return;
        }

        if (!RequestQue.addTask(cr)) {
            Logger.error("Closing request due to invalid structure.");
            Response failedResponse = ResponseFactory.MethodNotAllowed();
            try {
                StreamUtil.sendResponse(cr.getClientSocket().getOutputStream(), failedResponse);
            } catch (IOException e) {
                Logger.error("Unable to respond to the client: " + e.getMessage());
            }
            try {
                cr.getClientSocket().close();
            } catch (IOException e) {
                Logger.error("Unable to close connection to the client: " + e.getMessage());
            }
        }
    }
}
