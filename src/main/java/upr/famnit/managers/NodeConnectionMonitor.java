package upr.famnit.managers;

import upr.famnit.authentication.VerificationStatus;
import upr.famnit.components.LogLevel;
import upr.famnit.util.Logger;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

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
                node.closeConnection();
                Logger.warn("Closing node connection (" + node.getNodeName() + ") due to: " + status);
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
        if (node.getVerificationStatus() == VerificationStatus.Polling) {
            if (sinceLastPing.getSeconds() > Config.POLLING_NODE_CONNECTION_TIMEOUT) {
                return NodeStatus.Timeout;
            }
        }

        if (node.getVerificationStatus() == VerificationStatus.Working) {
            if (sinceLastPing.getSeconds() > Config.WORKING_NODE_CONNECTION_TIMEOUT) {
                return NodeStatus.Timeout;
            }
        }

        if (node.getVerificationStatus() == VerificationStatus.CompletedWork) {
            if (sinceLastPing.getSeconds() > Config.POLLING_NODE_CONNECTION_TIMEOUT) {
                return NodeStatus.Timeout;
            }
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

    public static TreeMap<String, Integer> getActiveConnections() {
        TreeMap<String, Integer> connectedNodes = new TreeMap<>();
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

    public static TreeMap<String, ArrayList<VerificationStatus>> getConnectionsStatus() {
        TreeMap<String, ArrayList<VerificationStatus>> connectedNodes = new TreeMap<>();
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

    public static TreeMap<String, ArrayList<String>> getLastPings() {
        TreeMap<String, ArrayList<String>> connectedNodes = new TreeMap<>();
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

    public static TreeMap<String, Set<String>> getTags() {
        TreeMap<String, Set<String>> connectedNodes = new TreeMap<>();
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
