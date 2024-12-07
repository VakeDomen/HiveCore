package upr.famnit.managers;

import upr.famnit.authentication.VerificationStatus;
import upr.famnit.components.LogLevel;
import upr.famnit.util.Logger;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import upr.famnit.util.Config;

public class NodeConnectionMonitor extends Thread {

    private volatile boolean monitoring = true;
    private static volatile ArrayList<NodeConnectionManager> nodes = new ArrayList<>();
    private static final Object nodeLock = new Object();

    public NodeConnectionMonitor() {
        nodes = new ArrayList<>();
    }



    @Override
    public void run() {
        Thread.currentThread().setName("Monitor");
        Logger.status("Monitor starting...");
        while (monitoring) {
            synchronized (nodeLock) {
                checkOnWorkers();
            }

            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }


        Logger.status("Monitor stopped.");
    }

    private void checkOnWorkers() {
        ArrayList<NodeConnectionManager> toRemove = new ArrayList<>();

        for (NodeConnectionManager node : nodes) {
            NodeStatus status = checkNode(node);

            // stop connection if violated any rules
            if (status != NodeStatus.Valid) {
                try {
                    Logger.warn("Closing node connection (" + node.getName() + ") due to: " + status);
                    node.closeConnection();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                toRemove.add(node);
            }
        }

        for (NodeConnectionManager nodeToRemove : toRemove) {
            nodes.remove(nodeToRemove);
        }
    }

    private NodeStatus checkNode(NodeConnectionManager node) {
        if (!node.isConnectionOpen()) {
            return NodeStatus.Closed;
        }

        // stop if node connection has been rejected (key-nonce mismatch)
        VerificationStatus nodeStatus = node.getVerificationStatus();
        if (nodeStatus == VerificationStatus.Rejected) {
            return NodeStatus.Rejected;
        }

        // attempt to verify node if waiting for verification
        if (nodeStatus == VerificationStatus.Waiting) {
            return verifyKeyAndNonce(node);
        }

        // check for timeouts
        LocalDateTime lastPing = node.getLastPing();
        LocalDateTime now = LocalDateTime.now();
        Duration sinceLastPing = Duration.between(lastPing, now);
        if (sinceLastPing.getSeconds() > Config.NODE_CONNECTION_TIMEOUT) {
            return NodeStatus.Timeout;
        }

        return NodeStatus.Valid;
    }

    private NodeStatus verifyKeyAndNonce(NodeConnectionManager node) {
        String nodeName = node.getNodeName();
        String nodeNonce = node.getNonce();

        for (NodeConnectionManager nodeConnectionManager : nodes) {
            if (nodeConnectionManager.getVerificationStatus() != VerificationStatus.Verified) {
                continue;
            }

            if (
                    nodeConnectionManager.getNodeName().equals(nodeName) &&
                    !nodeConnectionManager.getNonce().equals(nodeNonce)
            ) {
                return NodeStatus.InvalidNonce;
            }
        }

        node.setVerificationStatus(VerificationStatus.Verified);
        return NodeStatus.Valid;
    }


    public void addNode(NodeConnectionManager manager) {
        synchronized (nodeLock) {
            nodes.add(manager);
        }
    }

    public void stopMonitoring() {
        this.monitoring = false;
    }

    public static HashMap<String, Integer> getActiveConnections() {
        HashMap<String, Integer> connectedNodes = new HashMap<>();
        for (NodeConnectionManager node : nodes) {
            String name = node.getNodeName();
            if (name == null) {
                name = "Unauthenticated";
            }
            connectedNodes.putIfAbsent(name, 0);
            connectedNodes.put(name, connectedNodes.get(name) + 1);
        }
        return connectedNodes;
    }

    public static HashMap<String, ArrayList<VerificationStatus>> getConnectionsStatus() {
        HashMap<String, ArrayList<VerificationStatus>> connectedNodes = new HashMap<>();
        for (NodeConnectionManager node : nodes) {
            String name = node.getNodeName();
            if (name == null) {
                name = "Unauthenticated";
            }
            connectedNodes.putIfAbsent(name, new ArrayList<>());
            connectedNodes.get(name).add(node.getVerificationStatus());
        }
        return connectedNodes;
    }

    public static HashMap<String, ArrayList<String>> getLastPings() {
        HashMap<String, ArrayList<String>> connectedNodes = new HashMap<>();
        for (NodeConnectionManager node : nodes) {
            String name = node.getNodeName();
            if (name == null) {
                name = "Unauthenticated";
            }
            connectedNodes.putIfAbsent(name, new ArrayList<>());
            connectedNodes.get(node.getNodeName()).add(node.getLastPing().toString());
        }
        return connectedNodes;
    }

    public static HashMap<String, Set<String>> getTags() {
        HashMap<String, Set<String>> connectedNodes = new HashMap<>();
        for (NodeConnectionManager node : nodes) {
            String name = node.getNodeName();
            if (name == null) {
                name = "Unauthenticated";
            }
            connectedNodes.putIfAbsent(name, new HashSet<>());
            connectedNodes.get(node.getNodeName()).addAll(node.getTags());
        }
        return connectedNodes;
    }
}
