package upr.famnit.components;

import upr.famnit.util.Logger;
import upr.famnit.util.StreamUtil;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;

public class RequestQue {

    private static final HashMap<String, Queue<ClientRequest>> modelQue = new HashMap<>();
    private static final HashMap<String, Queue<ClientRequest>> nodeQue = new HashMap<>();

    public static synchronized ClientRequest getTask(String modelName, String nodeName) {
        Queue<ClientRequest> specificNodeQue = nodeQue.get(nodeName);
        if (specificNodeQue != null && !specificNodeQue.isEmpty()) {
            return specificNodeQue.poll();
        }

        Queue<ClientRequest> specificModelQue = modelQue.get(modelName);
        if (specificModelQue != null && !specificModelQue.isEmpty()) {
            return specificModelQue.poll();
        }
        return null;
    }

    public static synchronized boolean addTask(ClientRequest request) {
        if (request.getRequest().getProtocol().equals("HIVE")) {
            return false;
        }

        if (request.getRequest().getHeaders().containsKey("node"))
            return addToQueByNode(request);
        else
            return addToQueByModel(request);
    }

    private static synchronized boolean addToQueByModel(ClientRequest request) {
        String modelName = StreamUtil.getValueFromJSONBody("model", request.getRequest().getBody());
        Logger.log("Request for model: " + modelName);

        if (modelName == null) {
            Logger.log("Unable to determine target model for request.", LogLevel.error);
            return false;
        }

        modelQue.putIfAbsent(modelName, new LinkedList<>());
        Queue<ClientRequest> specificModelQue = modelQue.get(modelName);
        return specificModelQue.add(request);
    }

    private static synchronized boolean addToQueByNode(ClientRequest request) {
        String nodeName = request.getRequest().getHeaders().get("node");
        Logger.log("Request for worker node: " + nodeName);

        if (nodeName == null) {
            Logger.log("Unable to determine target node for request.", LogLevel.error);
            return false;
        }

        nodeQue.putIfAbsent(nodeName, new LinkedList<>());
        Queue<ClientRequest> specificNodeQue = nodeQue.get(nodeName);
        return specificNodeQue.add(request);
    }
}
