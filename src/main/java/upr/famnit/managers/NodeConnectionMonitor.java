package upr.famnit.managers;

import upr.famnit.components.LogLevel;
import upr.famnit.util.Logger;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;

import upr.famnit.util.Config;

public class NodeConnectionMonitor extends Thread {

    private boolean monitoring = true;
    private final ArrayList<NodeConnectionManager> nodes;

    public NodeConnectionMonitor() {
        nodes = new ArrayList<>();
    }

    @Override
    public void run() {
        Logger.log("Monitor starting...", LogLevel.status);
        while (monitoring) {
            LocalDateTime now = LocalDateTime.now();
            ArrayList<NodeConnectionManager> toRemove = new ArrayList<>();
            for (NodeConnectionManager node : nodes) {
                LocalDateTime lastPing = node.getLastPing();
                Duration sinceLastPing = Duration.between(lastPing, now);
                if (sinceLastPing.getSeconds() > Config.NODE_CONNECTION_TIMEOUT) {
                    try {
                        node.closeConnection();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    toRemove.add(node);
                }
            }

            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            for (NodeConnectionManager nodeToRemove : toRemove) {
                nodes.remove(nodeToRemove);
            }
        }
        Logger.log("Monitor stopped.", LogLevel.status);
    }


    public synchronized void addNode(NodeConnectionManager manager) {
        this.nodes.add(manager);
    }
}
