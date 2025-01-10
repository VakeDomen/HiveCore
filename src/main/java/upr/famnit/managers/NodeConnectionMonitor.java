package upr.famnit.managers;

import upr.famnit.authentication.VerificationStatus;
import upr.famnit.components.ClientRequest;
import upr.famnit.components.RequestQue;
import upr.famnit.components.ResponseFactory;
import upr.famnit.util.Logger;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

import upr.famnit.util.Config;
import upr.famnit.util.StreamUtil;

/**
 * The {@code NodeConnectionMonitor} class is responsible for monitoring and managing
 * the connections of worker nodes within the system. It ensures that all connected
 * nodes are authenticated, active, and responsive. Additionally, it manages the request
 * queue by rejecting unhandleable tasks and maintaining the integrity of node connections.
 *
 * <p>This class extends {@link Thread} and runs continuously to perform the following tasks:
 * <ul>
 *     <li>Checks the status of each connected worker node, handling timeouts and verification issues.</li>
 *     <li>Monitors the request queue to identify and reject tasks that cannot be handled by any node.</li>
 *     <li>Maintains a synchronized list of active node connections, ensuring thread-safe operations.</li>
 * </ul>
 * </p>
 *
 * <p>Thread safety is achieved through the use of synchronized blocks and a dedicated lock object
 * to manage access to shared resources. The class utilizes various logging mechanisms to track
 * its operations and any issues that arise during monitoring.</p>
 *
 * <p>Instances of {@code NodeConnectionMonitor} are intended to run as background threads,
 * continuously overseeing the health and performance of worker node connections until the
 * application is terminated.</p>
 *
 * @see NodeConnectionManager
 * @see ClientRequest
 * @see RequestQue
 * @see VerificationStatus
 */
public class NodeConnectionMonitor extends Thread {

    /**
     * Flag indicating whether the monitor is active and should continue running.
     * Setting this to {@code false} will gracefully stop the monitoring loop.
     */
    private volatile boolean monitoring = true;

    /**
     * A synchronized list of active {@link NodeConnectionManager} instances representing connected worker nodes.
     *
     * <p>Access to this list is controlled via the {@code nodeLock} object to ensure thread safety.</p>
     */
    private static volatile ArrayList<NodeConnectionManager> nodes = new ArrayList<>();

    /**
     * An object used as a lock for synchronizing access to the {@code nodes} list.
     */
    private static final Object nodeLock = new Object();

    /**
     * Constructs a new {@code NodeConnectionMonitor} instance.
     *
     * <p>Initializes the {@code nodes} list to ensure it's ready for managing worker connections.</p>
     */
    public NodeConnectionMonitor() {
        nodes = new ArrayList<>();
    }

    /**
     * The main execution method for the {@code NodeConnectionMonitor} thread.
     *
     * <p>This method performs the following actions in a continuous loop:
     * <ul>
     *     <li>Synchronizes access to the list of worker nodes and checks each node's status.</li>
     *     <li>Monitors the request queue for unhandleable tasks and rejects them accordingly.</li>
     *     <li>Pauses briefly between iterations to prevent excessive CPU usage.</li>
     * </ul>
     * </p>
     *
     * <p>The loop continues running as long as the {@code monitoring} flag is {@code true}.</p>
     */
    @Override
    public void run() {
        Thread.currentThread().setName("Monitor");
        Logger.status("Monitor starting...");
        while (monitoring) {
            synchronized (nodeLock) {
                checkOnWorkers();
            }

            checkOnQueue();

            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Logger.error("Monitor thread interrupted: " + e.getMessage() + " " + e.getMessage());
                Thread.currentThread().interrupt();
                break;
            }
        }

        Logger.status("Monitor stopped.");
    }

    /**
     * Checks the status of all connected worker nodes, handling any issues such as timeouts or verification failures.
     *
     * <p>This method iterates through the list of active nodes and performs the following checks:
     * <ul>
     *     <li>Determines if the node's connection is still open and valid.</li>
     *     <li>Handles nodes that have been rejected due to authentication issues.</li>
     *     <li>Identifies nodes that have exceeded allowed timeout durations.</li>
     * </ul>
     * </p>
     *
     * <p>Nodes that fail any of these checks are closed and removed from the active list.</p>
     */
    private void checkOnWorkers() {
        ArrayList<NodeConnectionManager> toRemove = new ArrayList<>();

        for (NodeConnectionManager node : nodes) {
            NodeStatus status = checkNode(node);

            // Stop connection if violated any rules
            if (status != NodeStatus.Valid) {
                node.closeConnection();
                Logger.warn("Closing node connection (" + node.getData().getNodeName() + ") due to: " + status);
                toRemove.add(node);
            }
        }

        for (NodeConnectionManager nodeToRemove : toRemove) {
            nodes.remove(nodeToRemove);
        }
    }

    /**
     * Checks the status of a single worker node and determines if any action is required.
     *
     * <p>This method performs the following checks on the given node:
     * <ul>
     *     <li>Verifies if the node's connection is still open.</li>
     *     <li>Handles nodes waiting for verification by ensuring key and nonce validity.</li>
     *     <li>Monitors for timeout conditions based on the node's current verification status.</li>
     * </ul>
     * </p>
     *
     * @param node the {@link NodeConnectionManager} representing the worker node to be checked
     * @return the {@link NodeStatus} indicating the result of the check
     */
    private NodeStatus checkNode(NodeConnectionManager node) {
        if (!node.isConnectionOpen()) {
            return NodeStatus.Closed;
        }

        // Stop node connection if it has been rejected (key-nonce mismatch)
        VerificationStatus nodeStatus = node.getData().getVerificationStatus();
        if (nodeStatus == VerificationStatus.Rejected) {
            return NodeStatus.Rejected;
        }

        // Attempt to verify node if waiting for verification
        if (nodeStatus == VerificationStatus.Waiting) {
            return verifyKeyAndNonce(node);
        }

        // Check for timeouts based on the node's current verification status
        LocalDateTime lastPing = node.getData().getLastPing();
        LocalDateTime now = LocalDateTime.now();
        Duration sinceLastPing = Duration.between(lastPing, now);
        long elapsedSeconds = sinceLastPing.getSeconds();

        switch (node.getData().getVerificationStatus()) {
            case Polling:
            case CompletedWork:
                if (elapsedSeconds > Config.POLLING_NODE_CONNECTION_TIMEOUT) {
                    return NodeStatus.Timeout;
                }
                break;
            case Working:
                if (elapsedSeconds > Config.WORKING_NODE_CONNECTION_TIMEOUT) {
                    return NodeStatus.Timeout;
                }
                break;
            default:
                // No action required for other statuses
                break;
        }

        return NodeStatus.Valid;
    }

    /**
     * Verifies the key and nonce of a worker node to ensure its authenticity.
     *
     * <p>This method performs the following actions:
     * <ul>
     *     <li>Retrieves the node's name and nonce.</li>
     *     <li>Checks for duplicate node names with different nonces to prevent key reuse.</li>
     *     <li>Updates the node's verification status based on the verification outcome.</li>
     * </ul>
     * </p>
     *
     * @param node the {@link NodeConnectionManager} representing the worker node to be verified
     * @return the {@link NodeStatus} indicating the result of the verification
     */
    private NodeStatus verifyKeyAndNonce(NodeConnectionManager node) {
        String nodeName = node.getData().getNodeName();
        String nodeNonce = node.getData().getNonce();

        for (NodeConnectionManager existingNode : nodes) {
            if (existingNode.getData().getVerificationStatus() != VerificationStatus.Verified) {
                continue;
            }

            // If the same node name exists with a different nonce, reject the connection
            if (existingNode.getData().getNodeName().equals(nodeName) &&
                    !existingNode.getData().getNonce().equals(nodeNonce)) {
                node.getData().setVerificationStatus(VerificationStatus.Rejected);
                return NodeStatus.InvalidNonce;
            }
        }

        node.getData().setVerificationStatus(VerificationStatus.Verified);
        return NodeStatus.Valid;
    }

    /**
     * Checks the request queue for unhandleable tasks and rejects them.
     *
     * <p>This method performs the following actions:
     * <ul>
     *     <li>Gathers the names of all active nodes and their supported models.</li>
     *     <li>Retrieves unhandleable tasks from the {@link RequestQue} based on active node capabilities.</li>
     *     <li>Sends a "Method Not Allowed" response to the client for each unhandleable task.</li>
     *     <li>Logs the rejection of each unhandleable request.</li>
     * </ul>
     * </p>
     */
    private void checkOnQueue() {
        ArrayList<String> nodeNames = new ArrayList<>();
        Set<String> modelNames = new HashSet<>();

        synchronized (nodeLock) {
            for (NodeConnectionManager node : nodes) {
                nodeNames.add(node.getName());
                modelNames.addAll(node.getTags());
            }
        }

        ClientRequest reqToReject = RequestQue.getUnhandlableTask(nodeNames, modelNames);
        while (reqToReject != null) {
            try {
                OutputStream os = reqToReject.getClientSocket().getOutputStream();
                StreamUtil.sendResponse(os, ResponseFactory.MethodNotAllowed());
                Logger.warn("Rejected unhandleable request: ");
                reqToReject.getRequest().log();
            } catch (IOException e) {
                Logger.error("Could not send rejection response to socket: " +
                        reqToReject.getClientSocket().getInetAddress());
            }
            reqToReject = RequestQue.getUnhandlableTask(nodeNames, modelNames);
        }
    }

    /**
     * Adds a new {@link NodeConnectionManager} to the list of monitored nodes.
     *
     * <p>This method ensures that the addition is thread-safe by synchronizing on the {@code nodeLock}.</p>
     *
     * @param manager the {@link NodeConnectionManager} instance representing the worker node to be added
     */
    public void addNode(NodeConnectionManager manager) {
        synchronized (nodeLock) {
            nodes.add(manager);
        }
    }

    /**
     * Stops the monitoring process by setting the {@code monitoring} flag to {@code false}.
     *
     * <p>This method allows the monitor thread to terminate gracefully after completing its current iteration.</p>
     */
    public void stopMonitoring() {
        this.monitoring = false;
    }

    /**
     * Retrieves a summary of active connections, mapping node names to their respective connection counts.
     *
     * @return a {@link TreeMap} mapping node names to the number of active connections for each node
     */
    public static TreeMap<String, Integer> getActiveConnections() {
        TreeMap<String, Integer> connectedNodes = new TreeMap<>();
        synchronized (nodeLock) {
            for (NodeConnectionManager node : nodes) {
                String name = node.getData().getNodeName();
                if (name == null) {
                    name = "Unauthenticated";
                }
                connectedNodes.putIfAbsent(name, 0);
                connectedNodes.put(name, connectedNodes.get(name) + 1);
            }
        }
        return connectedNodes;
    }

    /**
     * Retrieves the verification statuses of all active connections, mapping node names to their respective statuses.
     *
     * @return a {@link TreeMap} mapping node names to a list of their {@link VerificationStatus} values
     */
    public static TreeMap<String, ArrayList<VerificationStatus>> getConnectionsStatus() {
        TreeMap<String, ArrayList<VerificationStatus>> connectedNodes = new TreeMap<>();
        synchronized (nodeLock) {
            for (NodeConnectionManager node : nodes) {
                String name = node.getData().getNodeName();
                if (name == null) {
                    name = "Unauthenticated";
                }
                connectedNodes.putIfAbsent(name, new ArrayList<>());
                connectedNodes.get(name).add(node.getData().getVerificationStatus());
            }
        }
        return connectedNodes;
    }

    /**
     * Retrieves the last ping times of all active connections, mapping node names to their respective ping timestamps.
     *
     * @return a {@link TreeMap} mapping node names to a list of their last ping {@link String} representations
     */
    public static TreeMap<String, ArrayList<String>> getLastPings() {
        TreeMap<String, ArrayList<String>> connectedNodes = new TreeMap<>();
        synchronized (nodeLock) {
            for (NodeConnectionManager node : nodes) {
                String name = node.getData().getNodeName();
                if (name == null) {
                    continue;
                }
                connectedNodes.putIfAbsent(name, new ArrayList<>());
                connectedNodes.get(name).add(node.getData().getLastPing().toString());
            }
        }
        return connectedNodes;
    }

    /**
     * Retrieves the supported tags (models) of all active connections, mapping node names to their respective tag sets.
     *
     * @return a {@link TreeMap} mapping node names to a set of their supported tags (models)
     */
    public static TreeMap<String, Set<String>> getTags() {
        TreeMap<String, Set<String>> connectedNodes = new TreeMap<>();
        synchronized (nodeLock) {
            for (NodeConnectionManager node : nodes) {
                String name = node.getData().getNodeName();
                if (name == null) {
                    continue;
                }
                connectedNodes.putIfAbsent(name, new HashSet<>());
                connectedNodes.get(name).addAll(node.getTags());
            }
        }
        return connectedNodes;
    }

    /**
     * Retrieves the Ollama versions of all active connections, mapping node names to their respective version sets.
     *
     * @return a {@link TreeMap} mapping node names to a set of their Ollama versions
     */
    public static TreeMap<String, Set<String>> getOllamaVersions() {
        TreeMap<String, Set<String>> connectedNodes = new TreeMap<>();
        synchronized (nodeLock) {
            for (NodeConnectionManager node : nodes) {
                String name = node.getData().getNodeName();
                if (name == null) {
                    continue;
                }
                connectedNodes.putIfAbsent(name, new HashSet<>());
                String version = node.getData().getOllamaVersion();
                if (version == null) {
                    version = "Unknown";
                }
                connectedNodes.get(name).add(version);
            }
        }
        return connectedNodes;
    }

    /**
     * Retrieves the node versions of all active connections, mapping node names to their respective version sets.
     *
     * @return a {@link TreeMap} mapping node names to a set of their node versions
     */
    public static TreeMap<String, Set<String>> getNodeVersions() {
        TreeMap<String, Set<String>> connectedNodes = new TreeMap<>();
        synchronized (nodeLock) {
            for (NodeConnectionManager node : nodes) {
                String name = node.getData().getNodeName();
                if (name == null) {
                    continue;
                }
                connectedNodes.putIfAbsent(name, new HashSet<>());
                String version = node.getData().getNodeVersion();
                if (version == null) {
                    version = "Unknown";
                }
                connectedNodes.get(name).add(version);
            }
        }
        return connectedNodes;
    }
}
