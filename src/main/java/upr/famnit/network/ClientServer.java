package upr.famnit.network;

import upr.famnit.managers.ClientRequestManager;
import upr.famnit.util.LogLevel;
import upr.famnit.util.Logger;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static upr.famnit.util.Config.PROXY_PORT;

public class ClientServer implements Runnable {
    private ServerSocket serverSocket;
    private ExecutorService executorService;
    private NodeServer nodeServer;

    public ClientServer(NodeServer nodeServer) throws IOException {
        this.serverSocket = new ServerSocket(PROXY_PORT);
        this.executorService = Executors.newFixedThreadPool(8);
        this.nodeServer = nodeServer;
    }

    @Override
    public void run() {
        try {
            Logger.log("Proxy server is running on port " + PROXY_PORT + "...");

            while (true) {
                // Accept incoming client connection
                Socket clientSocket = serverSocket.accept();
                Logger.log("Client connected: " + clientSocket.getRemoteSocketAddress(), LogLevel.network);
                Socket nodeHandler = nodeServer.getNode().getNodeSocket();
                ClientRequestManager crm = new ClientRequestManager(clientSocket, nodeHandler);
                executorService.execute(crm);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }
}
