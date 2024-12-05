package upr.famnit.network;

import upr.famnit.components.LogLevel;
import upr.famnit.managers.NodeConnectionManager;
import upr.famnit.managers.NodeConnectionMonitor;
import upr.famnit.util.Logger;
import java.io.IOException;
import java.net.ServerSocket;

import static upr.famnit.util.Config.NODE_CONNECTION_PORT;

public class NodeServer implements Runnable {
    private final ServerSocket serverSocket;
    private final NodeConnectionMonitor monitor;

    public NodeServer() throws IOException {
        this.serverSocket = new ServerSocket(NODE_CONNECTION_PORT);
        this.monitor = new NodeConnectionMonitor();
    }

    @Override
    public void run() {
        Thread.currentThread().setName("WorkerServer");
        this.monitor.start();
        while (true) {
            try {
                Logger.network("Worker connection server is running on port " + NODE_CONNECTION_PORT + "...");
                NodeConnectionManager nodeConnection = new NodeConnectionManager(serverSocket);
                nodeConnection.start();
                this.monitor.addNode(nodeConnection);
            } catch (IOException e) {
                Logger.error("Something went wrong accepting worker connection: " + e.getMessage());
            }
        }

//        try {
//            Logger.status("Waiting for monitor to stop...");
//            this.monitor.stopMonitoring();
//            this.monitor.join();
//        } catch (InterruptedException e) {
//            throw new RuntimeException(e);
//        }
    }
}
