package upr.famnit.network;

import upr.famnit.components.*;
import upr.famnit.managers.ClientConnectionManager;
import upr.famnit.util.Logger;

import java.io.IOException;
import java.net.ServerSocket;

import static upr.famnit.util.Config.PROXY_PORT;

public class ClientServer implements Runnable {

    private final ServerSocket serverSocket;

    public ClientServer() throws IOException {
        this.serverSocket = new ServerSocket(PROXY_PORT);
    }

    @Override
    public void run() {
        Thread.currentThread().setName("ClientServer");
        while (true) {
            try {
                Logger.network("Proxy server is running on port " + PROXY_PORT + "...");
                ClientConnectionManager connection = new ClientConnectionManager(serverSocket);
                connection.start();
            } catch (IOException e) {
                Logger.error("Something went wrong accepting client connection: " + e.getMessage());
            }
        }
    }
}
