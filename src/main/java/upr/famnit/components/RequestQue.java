package upr.famnit.components;

import upr.famnit.util.Logger;
import upr.famnit.util.StreamUtil;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

public class RequestQue {

    private static HashMap<String, List<ClientRequest>> queue = new HashMap<>();

    public static synchronized ClientRequest getTask(String modelName) {
        List<ClientRequest> modelQue = queue.get(modelName);
        ClientRequest task = null;
        if (modelQue != null && !modelQue.isEmpty()) {
            task = modelQue.removeFirst();
        }
        return task;
    }

    public static synchronized boolean addTask(ClientRequest request) {
        if (request.getRequest().getProtocol().equals("HIVE")) {
            return false;
        }

        String modelName = StreamUtil.getValueFromJSONBody("model", request.getRequest().getBody());
        Logger.log("Request for model: " + modelName);

        if (modelName == null) {
            Logger.log("Unable to determine target model for request.", LogLevel.error);
            return false;
        }

        queue.putIfAbsent(modelName, new LinkedList<>());
        List<ClientRequest> modelQue = queue.get(modelName);
        modelQue.add(request);
        return true;
    }

}
