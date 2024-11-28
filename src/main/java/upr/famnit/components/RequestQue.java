package upr.famnit.components;

import upr.famnit.util.Logger;
import upr.famnit.util.StreamUtil;

import java.util.HashMap;
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
                return request;
            }
        }

        ConcurrentLinkedQueue<ClientRequest> specificModelQue = modelQue.get(modelName);
        if (specificModelQue != null) {
            return specificModelQue.poll();
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
        request.stamp();
        String modelName = StreamUtil.getValueFromJSONBody("model", request.getRequest().getBody());
        Logger.log("Request for model: " + modelName);

        if (modelName == null) {
            Logger.log("Unable to determine target model for request.", LogLevel.error);
            return false;
        }

        modelQue.computeIfAbsent(modelName, k -> new ConcurrentLinkedQueue<>()).add(request);
        return true;
    }

    private static boolean addToQueByNode(ClientRequest request) {
        request.stamp();
        String nodeName = request.getRequest().getHeaders().get("node");
        Logger.log("Request for worker node: " + nodeName);

        if (nodeName == null) {
            Logger.log("Unable to determine target node for request.", LogLevel.error);
            return false;
        }

        nodeQue.computeIfAbsent(nodeName, k -> new ConcurrentLinkedQueue<>()).add(request);
        return true;
    }

    public static HashMap<String, Integer> getQueLengths() {
        HashMap<String, Integer> queLengths = new HashMap<>();
        modelQue.forEach((model, queue) -> queLengths.put("Model: " + model, queue.size()));
        nodeQue.forEach((node, queue) -> queLengths.put("Node: " + node, queue.size()));
        return queLengths;
    }
}
