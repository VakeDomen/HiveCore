package upr.famnit.managers;

import upr.famnit.components.LogLevel;
import upr.famnit.util.Logger;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;

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
        Thread.currentThread().setName("Monitor");
        while (monitoring) {
            LocalDateTime now = LocalDateTime.now();
            ArrayList<NodeConnectionManager> toRemove = new ArrayList<>();
            HashMap<String, NodeConnectionManager> nodeNames = new HashMap<>();
            for (NodeConnectionManager node : nodes) {
                boolean shouldClose = false;

                // check for same-key usage
                if (!nodeNames.containsKey(node.getName())) {
                    nodeNames.put(node.getName(), node);
                }

                NodeConnectionManager existingNode = nodeNames.get(node.getName());
                if (existingNode != node) {
                    shouldClose = node.getLastPing().isBefore(existingNode.getLastPing());
                }


                // check for timeouts
                LocalDateTime lastPing = node.getLastPing();
                Duration sinceLastPing = Duration.between(lastPing, now);
                if (sinceLastPing.getSeconds() > Config.NODE_CONNECTION_TIMEOUT) {
                    shouldClose = true;
                }

                // stop connection if violated any rules
                if (shouldClose) {
                    try {
                        Logger.log("Closing node connection: " + node.getName(), LogLevel.warn);
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

    public void stopMonitoring() {
        this.monitoring = false;
    }
}
