package upr.famnit.network;

import upr.famnit.components.*;
import upr.famnit.managers.NodeConnectionManager;
import upr.famnit.managers.NodeConnectionMonitor;
import upr.famnit.managers.ProxyManager;
import upr.famnit.util.Logger;
import upr.famnit.util.StreamUtil;

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
        try {
            Logger.log("Management connection server is running on port " + MANAGEMENT_CONNECTION_PORT + "...");
            while (true) {
                Logger.log("Waiting for management request... ", LogLevel.network);

                Socket clientSocket = serverSocket.accept();
                Logger.log("Management request received: ", LogLevel.network);
                executorService.execute(new ProxyManager(clientSocket));

            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
