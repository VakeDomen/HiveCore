package upr.famnit.components;

import upr.famnit.util.Logger;
import upr.famnit.util.StreamUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;

public class RequestQue {

    private static final ConcurrentMap<String, ConcurrentLinkedQueue<ClientRequest>> modelQue = new ConcurrentHashMap<>();
    private static final ConcurrentMap<String, ConcurrentLinkedQueue<ClientRequest>> nodeQue = new ConcurrentHashMap<>();

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

    public static HashMap<String, Integer> getQueLengths() {
        HashMap<String, Integer> queLengths = new HashMap<>();
        modelQue.forEach((model, queue) -> queLengths.put("Model: " + model, queue.size()));
        nodeQue.forEach((node, queue) -> queLengths.put("Node: " + node, queue.size()));
        return queLengths;
    }

    public static ClientRequest getUnhandlableTask(ArrayList<String> nodeNames, Set<String> modelNames) {
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
