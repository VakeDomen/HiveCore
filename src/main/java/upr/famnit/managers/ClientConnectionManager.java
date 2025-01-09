package upr.famnit.managers;

import upr.famnit.components.*;
import upr.famnit.util.Logger;
import upr.famnit.util.StreamUtil;

import javax.security.sasl.AuthenticationException;
import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class ClientConnectionManager implements Runnable {

    private final Socket clientSocket;

    public ClientConnectionManager(ServerSocket clientServer) throws IOException {
        this.clientSocket = clientServer.accept();
    }

    @Override
    public void run() {
        ClientRequest cr = null;
        try {
            cr = new ClientRequest(clientSocket);
        } catch (AuthenticationException e) {
            Logger.error("Unauthorized client request(" + clientSocket.getRemoteSocketAddress() + "): " + e.getMessage());
            this.rejectRequest(clientSocket);
            return;
        } catch (IOException e) {
            Logger.error("Failed reading client request(" + clientSocket.getRemoteSocketAddress() + "): " + e.getMessage());
            return;
        }


        if (!RequestQue.addTask(cr)) {
            Logger.error("Closing request due to invalid structure.(" + clientSocket.getRemoteSocketAddress() + ")");
            Response failedResponse = ResponseFactory.MethodNotAllowed();
            try {
                StreamUtil.sendResponse(cr.getClientSocket().getOutputStream(), failedResponse);
            } catch (IOException e) {
                Logger.error("Unable to respond to the client: (" + clientSocket.getRemoteSocketAddress() + ")" + e.getMessage());
            }
            try {
                cr.getClientSocket().close();
            } catch (IOException e) {
                Logger.error("Unable to close connection to the client(" + clientSocket.getRemoteSocketAddress() + "): " + e.getMessage());
            }
            cr.getRequest().log();
        }
    }

    private void rejectRequest(Socket clientSocket) {
        try {
            OutputStream os = clientSocket.getOutputStream();
            StreamUtil.sendResponse(os, ResponseFactory.Unauthorized());
        } catch (IOException e) {
            Logger.error("Could not send rejection response to socket: " + clientSocket.getInetAddress());
        }
    }
}
