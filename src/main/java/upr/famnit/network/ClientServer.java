package upr.famnit.network;

import upr.famnit.components.*;
import upr.famnit.util.Logger;
import upr.famnit.util.StreamUtil;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import static upr.famnit.util.Config.PROXY_PORT;

public class ClientServer implements Runnable {

    private final ServerSocket serverSocket;

    public ClientServer() throws IOException {
        this.serverSocket = new ServerSocket(PROXY_PORT);
    }

    @Override
    public void run() {
        Thread.currentThread().setName("ClientServer");
        try {
            Logger.log("Proxy server is running on port " + PROXY_PORT + "...", LogLevel.network);
            while (true) {
                // Accept incoming client connection
                Socket clientSocket = serverSocket.accept();
                Logger.log("Client connected: " + clientSocket.getRemoteSocketAddress(), LogLevel.network);
                ClientRequest cr = new ClientRequest(clientSocket);

                if (!RequestQue.addTask(cr)) {
                    Response failedResponse = ResponseFactory.MethodNotAllowed();
                    StreamUtil.sendResponse(cr.getClientSocket().getOutputStream(), failedResponse);
                    cr.getClientSocket().close();
                    Logger.log("Closing request due to invalid structure.", LogLevel.network);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
