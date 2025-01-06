package upr.famnit.managers;

import upr.famnit.authentication.VerificationStatus;
import upr.famnit.util.Logger;

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
            if (status != NodeStatus.Timeout) {
                node.closeConnection();
                Logger.warn("Closing node connection (" + node.getData().getNodeName() + ") due to: " + status);
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
        VerificationStatus nodeStatus = node.getData().getVerificationStatus();
        if (nodeStatus == VerificationStatus.Rejected) {
            return NodeStatus.Rejected;
        }

        // attempt to verify node if waiting for verification
        if (nodeStatus == VerificationStatus.Waiting) {
            return verifyKeyAndNonce(node);
        }

        // check for timeouts
        LocalDateTime lastPing = node.getData().getLastPing();
        LocalDateTime now = LocalDateTime.now();
        Duration sinceLastPing = Duration.between(lastPing, now);
        if (node.getData().getVerificationStatus() == VerificationStatus.Polling) {
            if (sinceLastPing.getSeconds() > Config.POLLING_NODE_CONNECTION_TIMEOUT) {
                return NodeStatus.Timeout;
            }
        }

        if (node.getData().getVerificationStatus() == VerificationStatus.Working) {
            if (sinceLastPing.getSeconds() > Config.WORKING_NODE_CONNECTION_TIMEOUT) {
                return NodeStatus.Timeout;
            }
        }

        if (node.getData().getVerificationStatus() == VerificationStatus.CompletedWork) {
            if (sinceLastPing.getSeconds() > Config.POLLING_NODE_CONNECTION_TIMEOUT) {
                return NodeStatus.Timeout;
            }
        }

        return NodeStatus.Valid;
    }

    private NodeStatus verifyKeyAndNonce(NodeConnectionManager node) {
        String nodeName = node.getData().getNodeName();
        String nodeNonce = node.getData().getNonce();

        for (NodeConnectionManager nodeConnectionManager : nodes) {
            if (nodeConnectionManager.getData().getVerificationStatus() != VerificationStatus.Verified) {
                continue;
            }

            // if same node name, different nonce
            if (
                    nodeConnectionManager.getData().getNodeName().equals(nodeName) &&
                    !nodeConnectionManager.getData().getNonce().equals(nodeNonce)
            ) {
                node.getData().setVerificationStatus(VerificationStatus.Rejected);
                return NodeStatus.InvalidNonce;
            }
        }

        node.getData().setVerificationStatus(VerificationStatus.Verified);
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
            String name = node.getData().getNodeName();
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
            String name = node.getData().getNodeName();
            if (name == null) {
                name = "Unauthenticated";
            }
            connectedNodes.putIfAbsent(name, new ArrayList<>());
            connectedNodes.get(name).add(node.getData().getVerificationStatus());
        }
        return connectedNodes;
    }

    public static TreeMap<String, ArrayList<String>> getLastPings() {
        TreeMap<String, ArrayList<String>> connectedNodes = new TreeMap<>();
        for (NodeConnectionManager node : nodes) {
            String name = node.getData().getNodeName();
            if (name == null) {
                name = "Unauthenticated";
            }
            connectedNodes.putIfAbsent(name, new ArrayList<>());
            connectedNodes.get(node.getData().getNodeName()).add(node.getData().getLastPing().toString());
        }
        return connectedNodes;
    }

    public static TreeMap<String, Set<String>> getTags() {
        TreeMap<String, Set<String>> connectedNodes = new TreeMap<>();
        for (NodeConnectionManager node : nodes) {
            String name = node.getData().getNodeName();
            if (name == null) {
                name = "Unauthenticated";
            }
            connectedNodes.putIfAbsent(name, new HashSet<>());
            connectedNodes.get(node.getData().getNodeName()).addAll(node.getTags());
        }
        return connectedNodes;
    }

    public static TreeMap<String, Set<String>> getOllamaVersions() {
        TreeMap<String, Set<String>> connectedNodes = new TreeMap<>();
        for (NodeConnectionManager node : nodes) {
            String name = node.getData().getNodeName();
            if (name == null) {
                name = "Unauthenticated";
            }
            connectedNodes.putIfAbsent(name, new HashSet<>());
            String version = node.getData().getOllamaVersion();
            if (version == null) {
                version = "Unknown";
            }
            connectedNodes.get(node.getData().getNodeName()).add(version);
        }
        return connectedNodes;
    }

    public static TreeMap<String, Set<String>> getNodeVersions() {
        TreeMap<String, Set<String>> connectedNodes = new TreeMap<>();
        for (NodeConnectionManager node : nodes) {
            String name = node.getData().getNodeName();
            if (name == null) {
                name = "Unauthenticated";
            }
            connectedNodes.putIfAbsent(name, new HashSet<>());
            String version = node.getData().getNodeVersion();
            if (version == null) {
                version = "Unknown";
            }
            connectedNodes.get(node.getData().getNodeName()).add(version);
        }
        return connectedNodes;
    }
}
