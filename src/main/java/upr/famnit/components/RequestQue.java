package upr.famnit.components;

import upr.famnit.util.Logger;
import upr.famnit.util.StreamUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;

/**
 * The {@code RequestQue} class manages client requests by organizing them into queues
 * based on either model names or node names. It provides functionality to add tasks
 * to the appropriate queues and retrieve tasks for processing.
 */
public class RequestQue {

    /**
     * A concurrent map that holds queues of {@link ClientRequest} objects keyed by model name.
     */
    private static final ConcurrentMap<String, ConcurrentLinkedQueue<ClientRequest>> modelQue = new ConcurrentHashMap<>();

    /**
     * A concurrent map that holds queues of {@link ClientRequest} objects keyed by node name.
     */
    private static final ConcurrentMap<String, ConcurrentLinkedQueue<ClientRequest>> nodeQue = new ConcurrentHashMap<>();

    /**
     * Retrieves a client request task based on the specified model and node names.
     *
     * <p>The method first attempts to fetch a task from the queue associated with the
     * provided node name. If no task is found, it then attempts to retrieve a task
     * from the queue associated with the model name.</p>
     *
     * @param modelName the name of the model associated with the request
     * @param nodeName the name of the node associated with the request
     * @return the next {@link ClientRequest} if available; otherwise, {@code null}
     */
    public static ClientRequest getTask(String modelName, String nodeName) {
        ConcurrentLinkedQueue<ClientRequest> specificNodeQue = nodeQue.get(nodeName);
        if (specificNodeQue != null) {
            ClientRequest request = specificNodeQue.poll();
            if (request != null) {
                request.stampQueueLeave(nodeName);
                return request;
            }
        }

        ConcurrentLinkedQueue<ClientRequest> specificModelQue = modelQue.get(modelName);
        if (specificModelQue != null) {
            ClientRequest request = specificModelQue.poll();
            if (request != null) {
                request.stampQueueLeave(nodeName);
                return request;
            }
        }
        return null;
    }

    public static ClientRequest getNodeTask(String nodeName) {
        ConcurrentLinkedQueue<ClientRequest> specificNodeQue = nodeQue.get(nodeName);
        if (specificNodeQue != null) {
            ClientRequest request = specificNodeQue.poll();
            if (request != null) {
                request.stampQueueLeave(nodeName);
                return request;
            }
        }

        return null;
    }

    public static ClientRequest getModelTask(String modelName, String nodeName) {
        ConcurrentLinkedQueue<ClientRequest> specificModelQue = modelQue.get(modelName);
        if (specificModelQue != null) {
            ClientRequest request = specificModelQue.poll();
            if (request != null) {
                request.stampQueueLeave(nodeName);
                return request;
            }
        }
        return null;
    }

    /**
     * Adds a client request task to the appropriate queue based on its headers.
     *
     * <p>If the request protocol is "HIVE", the task is not added to any queue.
     * If the request contains a "node" header, it is added to the node-specific queue.
     * Otherwise, it is added to the model-specific queue.</p>
     *
     * @param request the {@link ClientRequest} to be added
     * @return {@code true} if the task was successfully added; {@code false} otherwise
     */
    public static boolean addTask(ClientRequest request) {
        if (request.getRequest().getProtocol().equals("HIVE")) {
            return false;
        }

        if (request.getRequest().getHeaders().containsKey("node")) {
            return addToQueByNode(request);
        } else {
            return addToQueByModel(request);
        }
    }

    public static boolean addHiveTask(ClientRequest request, String worker) {
        return addToQueByNode(request, worker);
    }

    /**
     * Adds a client request task to the model-specific queue.
     *
     * <p>The method extracts the model name from the request body and adds the
     * request to the corresponding queue. If the model name cannot be determined,
     * a warning is logged and the task is not added.</p>
     *
     * @param request the {@link ClientRequest} to be added
     * @return {@code true} if the task was successfully added; {@code false} otherwise
     */
    private static boolean addToQueByModel(ClientRequest request) {
        request.stampQueueEnter();
        String modelName = StreamUtil.getValueFromJSONBody("model", request.getRequest().getBody());

        if (modelName == null) {
            Logger.warn("Unable to determine target model for request.");
            return false;
        }

        modelQue.computeIfAbsent(modelName, k -> new ConcurrentLinkedQueue<>()).add(request);

        Logger.info("Request for model " + modelName + " added to the queue. (" + request.getClientSocket().getRemoteSocketAddress() + ")");
        return true;
    }

    /**
     * Adds a client request task to the node-specific queue.
     *
     * <p>The method retrieves the node name from the request headers and adds the
     * request to the corresponding queue. If the node name is not specified, a
     * warning is logged and the task is not added.</p>
     *
     * @param request the {@link ClientRequest} to be added
     * @return {@code true} if the task was successfully added; {@code false} otherwise
     */
    private static boolean addToQueByNode(ClientRequest request) {
        request.stampQueueEnter();
        String nodeName = request.getRequest().getHeaders().get("node");

        if (nodeName == null) {
            Logger.warn("Unable to determine target node for request.");
            return false;
        }

        nodeQue.computeIfAbsent(nodeName, k -> new ConcurrentLinkedQueue<>()).add(request);
        Logger.info("Request for worker node " + nodeName + " added to the queue. (" + request.getClientSocket().getRemoteSocketAddress() + ")");
        return true;
    }

    private static boolean addToQueByNode(ClientRequest request, String nodeName) {
        request.stampQueueEnter();

        if (nodeName == null) {
            Logger.warn("Unable to determine target node for request.");
            return false;
        }

        nodeQue.computeIfAbsent(nodeName, k -> new ConcurrentLinkedQueue<>()).add(request);
        Logger.info("Request for worker node " + nodeName + " added to the queue. (" + request.getClientSocket().getRemoteSocketAddress() + ")");
        return true;
    }

    /**
     * Retrieves the lengths of all model and node queues.
     *
     * <p>The method returns a {@link HashMap} where each key is prefixed with
     * "Model: " or "Node: " to indicate the type of queue, and the value is the
     * number of tasks in that queue.</p>
     *
     * @return a {@link HashMap} containing the lengths of each queue
     */
    public static HashMap<String, Integer> getQueLengths() {
        HashMap<String, Integer> queLengths = new HashMap<>();
        modelQue.forEach((model, queue) -> queLengths.put("Model: " + model, queue.size()));
        nodeQue.forEach((node, queue) -> queLengths.put("Node: " + node, queue.size()));
        return queLengths;
    }

    /**
     * Retrieves an unhandleable client request task based on provided node and model names.
     *
     * <p>The method searches for a task in the node queues excluding the specified node names.
     * If no suitable task is found, it searches the model queues excluding the specified model names.
     * The first matching task found is returned.</p>
     *
     * @param nodeNames a list of node names to exclude from the search
     * @param modelNames a set of model names to exclude from the search
     * @return a {@link ClientRequest} if an unhandleable task is found; otherwise, {@code null}
     */
    public static ClientRequest getUnhandlableTask(ArrayList<String> nodeNames, Set<String> modelNames) {
        // Search node queues excluding specified nodes
        for (String nodeName : nodeQue.keySet()) {
            if (nodeNames.contains(nodeName)) {
                continue;
            }

            ConcurrentLinkedQueue<ClientRequest> specificNodeQue = nodeQue.get(nodeName);
            if (specificNodeQue == null) {
                continue;
            }

            ClientRequest req = specificNodeQue.poll();
            if (req != null) {
                return req;
            }
        }

        // Search model queues excluding specified models
        for (String modelName : modelQue.keySet()) {
            if (modelNames.contains(modelName)) {
                continue;
            }

            ConcurrentLinkedQueue<ClientRequest> specificModelQue = modelQue.get(modelName);
            if (specificModelQue == null) {
                continue;
            }

            ClientRequest req = specificModelQue.poll();
            if (req != null) {
                return req;
            }
        }

        return null;
    }

}
