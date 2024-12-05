package upr.famnit.network;

import upr.famnit.managers.ClientConnectionManager;
import upr.famnit.util.Logger;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static upr.famnit.util.Config.PROXY_PORT;

public class ClientServer implements Runnable {
    private final ExecutorService requestThreadExecutor;
    private final ServerSocket serverSocket;

    public ClientServer() throws IOException {
        this.serverSocket = new ServerSocket(PROXY_PORT);
        this.requestThreadExecutor = Executors.newCachedThreadPool();
    }

    @Override
    public void run() {
        Thread.currentThread().setName("ClientServer");
        Logger.network("Proxy server is running on port " + PROXY_PORT + "...");

        while (true) {
            try {
                ClientConnectionManager connection = new ClientConnectionManager(serverSocket);
                requestThreadExecutor.submit(connection);
            } catch (IOException e) {
                Logger.error("Something went wrong accepting client connection: " + e.getMessage());
            }
        }
    }
}
