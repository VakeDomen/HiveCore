package upr.famnit.network;

import upr.famnit.managers.ProxyManager;
import upr.famnit.util.Logger;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static upr.famnit.util.Config.MANAGEMENT_CONNECTION_PORT;

public class ManagementServer implements Runnable {
    private final ServerSocket serverSocket;
    private final ExecutorService executorService;

    public ManagementServer() throws IOException {
        this.serverSocket = new ServerSocket(MANAGEMENT_CONNECTION_PORT);
        this.executorService = Executors.newCachedThreadPool();
    }

    @Override
    public void run() {
        Thread.currentThread().setName("ManagementServer");
        Logger.network("Management connection server is running on port " + MANAGEMENT_CONNECTION_PORT + "...");

        while (true) {
            try {
                Socket clientSocket = serverSocket.accept();
                executorService.execute(new ProxyManager(clientSocket));
            } catch (IOException e) {
                Logger.error("Something went wrong accepting management connection: " + e.getMessage());
            }
        }
    }
}
