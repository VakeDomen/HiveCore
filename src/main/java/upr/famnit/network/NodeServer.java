package upr.famnit.network;

import upr.famnit.components.LogLevel;
import upr.famnit.managers.NodeConnectionManager;
import upr.famnit.managers.NodeConnectionMonitor;
import upr.famnit.util.Logger;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static upr.famnit.util.Config.NODE_CONNECTION_PORT;

public class NodeServer implements Runnable {
    private final ExecutorService workerPool;
    private final ServerSocket serverSocket;
    private final NodeConnectionMonitor monitor;

    public NodeServer() throws IOException {
        this.serverSocket = new ServerSocket(NODE_CONNECTION_PORT);
        this.monitor = new NodeConnectionMonitor();
        this.workerPool = Executors.newCachedThreadPool();
    }

    @Override
    public void run() {
        Thread.currentThread().setName("WorkerServer");
        this.monitor.start();
        Logger.network("Worker connection server is running on port " + NODE_CONNECTION_PORT + "...");

        while (true) {
            try {
                NodeConnectionManager nodeConnection = new NodeConnectionManager(serverSocket);
                workerPool.submit(nodeConnection);
                this.monitor.addNode(nodeConnection);
            } catch (IOException e) {
                Logger.error("Something went wrong accepting worker connection: " + e.getMessage());
            }
        }
    }
}
