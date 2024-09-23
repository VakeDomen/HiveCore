package upr.famnit.network;

import upr.famnit.components.ClientRequest;
import upr.famnit.components.LogLevel;
import upr.famnit.components.RequestQue;
import upr.famnit.util.Logger;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import static upr.famnit.util.Config.PROXY_PORT;

public class ClientServer implements Runnable {
    private ServerSocket serverSocket;

    public ClientServer(NodeServer nodeServer) throws IOException {
        this.serverSocket = new ServerSocket(PROXY_PORT);
    }

    @Override
    public void run() {
        try {
            Logger.log("Proxy server is running on port " + PROXY_PORT + "...");
            while (true) {
                // Accept incoming client connection
                Socket clientSocket = serverSocket.accept();
                Logger.log("Client connected: " + clientSocket.getRemoteSocketAddress(), LogLevel.network);
                ClientRequest cr = new ClientRequest(clientSocket);
                RequestQue.addTask(cr);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }
}
