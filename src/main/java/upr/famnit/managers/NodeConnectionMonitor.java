package upr.famnit.managers;

import upr.famnit.authentication.VerificationStatus;
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

            ArrayList<NodeConnectionManager> toRemove = new ArrayList<>();
            for (NodeConnectionManager node : nodes) {
                NodeStatus status = checkNode(node);

                // stop connection if violated any rules
                if (status != NodeStatus.Valid) {
                    try {
                        Logger.log("Closing node connection (" + node.getName() + ") due to: " + status, LogLevel.warn);
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

    private NodeStatus checkNode(NodeConnectionManager node) {
        if (!node.isConnectionOpen()) {
            return NodeStatus.Closed;
        }

        // stop if node connection has been rejected (key-nonce mismatch)
        VerificationStatus nodeStatus = node.getVerificationStatus();
        if (nodeStatus == VerificationStatus.Rejected) {
            return NodeStatus.PastRejection;
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


    public synchronized void addNode(NodeConnectionManager manager) {
        this.nodes.add(manager);
    }

    public void stopMonitoring() {
        this.monitoring = false;
    }
}
