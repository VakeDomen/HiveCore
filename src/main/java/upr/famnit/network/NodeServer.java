package upr.famnit.network;

import upr.famnit.managers.NodeConnectionManager;
import upr.famnit.util.Logger;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;

import static upr.famnit.util.Config.NODE_CONNECTION_PORT;

public class NodeServer implements Runnable {
    private final ServerSocket serverSocket;
    private final Object nodesLock = new Object();
    private final ArrayList<NodeConnectionManager> nodes;


    public NodeServer() throws IOException {
        this.serverSocket = new ServerSocket(NODE_CONNECTION_PORT);
        this.nodes = new ArrayList<>();
    }

    @Override
    public void run() {
        try {
            Logger.log("Proxy server is running on port " + NODE_CONNECTION_PORT + "...");

            while (true) {
                NodeConnectionManager nodeConnection = new NodeConnectionManager();
                nodeConnection.acceptConnection(serverSocket);
                addNode(nodeConnection);
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void addNode(NodeConnectionManager node) {
        synchronized (nodesLock) {
            this.nodes.add(node);
        }
    }

    public NodeConnectionManager getNode() {
        synchronized (nodesLock) {
            return this.nodes.getFirst();
        }
    }

}
