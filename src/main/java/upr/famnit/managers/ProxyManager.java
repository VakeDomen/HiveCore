package upr.famnit.managers;

import com.google.gson.Gson;
import upr.famnit.authentication.Key;
import upr.famnit.authentication.KeyUtil;
import upr.famnit.authentication.Role;
import upr.famnit.authentication.SubmittedKey;
import upr.famnit.components.ClientRequest;
import upr.famnit.components.LogLevel;
import upr.famnit.components.Response;
import upr.famnit.components.ResponseFactory;
import upr.famnit.util.Logger;
import upr.famnit.util.StreamUtil;

import java.io.IOException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ProxyManager implements Runnable {

    private final Socket clientSocket;
    private ClientRequest clientRequest;

    public ProxyManager(Socket clientSocket) {
        this.clientSocket = clientSocket;
    }

    @Override
    public void run() {
        Thread.currentThread().setName("Proxy manager");
        try {
            clientRequest = new ClientRequest(clientSocket);
        } catch (IOException e) {
            Logger.log("Error reading management request: " + e.getMessage(), LogLevel.error);
            return;
        }

        try {

            if (!isAdminRequest()) {
                respond(ResponseFactory.Unauthorized());
                return;
            }

            switch (clientRequest.getRequest().getUri()) {
                case "/key" -> handleKeyRoute();
                case "/worker" -> handleWorkerRoute();
                case null, default -> respond(ResponseFactory.NotFound());
            }

        } catch (IOException e) {
            Logger.log("Error handling proxy management request: " + e.getMessage(), LogLevel.error);
            throw new RuntimeException(e);

        }
        Logger.log("Management request finished");
    }

    private void handleKeyRoute() throws IOException {
        switch (clientRequest.getRequest().getMethod()) {
            case "GET" -> handleListKeysRequest();
            case "POST" -> handleInsertKeyRequest();
            case null, default -> respond(ResponseFactory.NotFound());
        }
    }

    private void handleWorkerRoute() throws IOException {
        switch (clientRequest.getRequest().getMethod()) {
            case "GET" -> handleActiveWorkersRequest();
            case null, default -> respond(ResponseFactory.NotFound());
        }
    }

    private void handleActiveWorkersRequest() throws IOException {
        HashMap<String, Integer> activeConnections = NodeConnectionMonitor.getActiveConnections();
        Gson gson = new Gson();
        String body = gson.toJson(activeConnections);
        respond(ResponseFactory.Ok(body.getBytes(StandardCharsets.UTF_8)));
    }

    private void handleInsertKeyRequest() throws IOException {
        Gson gson = new Gson();
        String body = new String(clientRequest.getRequest().getBody(), StandardCharsets.UTF_8);
        SubmittedKey submittedKey = gson.fromJson(body, SubmittedKey.class);
        Key validKey = new Key(submittedKey);

        try {
            DatabaseManager.insertKey(validKey);
        } catch (SQLException e) {
            Logger.log("Something went wrong generating new key: " + e.getMessage(), LogLevel.error);
            respond(ResponseFactory.BadRequest());
        }

        respond(ResponseFactory.Ok(validKey.getValue().getBytes(StandardCharsets.UTF_8)));
    }

    private void handleListKeysRequest() throws IOException {
        ArrayList<Key> keys = null;
        try {
             keys = DatabaseManager.getAllKeys();
        } catch (SQLException e) {
            Logger.log("Something went wrong fetching keys: " + e.getMessage(), LogLevel.error);
            respond(ResponseFactory.BadRequest());
        }

        if (keys == null || keys.isEmpty()) {
            respond(ResponseFactory.NotFound());
            return;
        }

        Gson gson = new Gson();
        String body = gson.toJson(keys);
        respond(ResponseFactory.Ok(body.getBytes(StandardCharsets.UTF_8)));
    }

    private boolean isAdminRequest() {
        Map<String, String> headers = clientRequest.getRequest().getHeaders();
        if (headers == null || headers.isEmpty()) {
            return false;
        }

        String authHeader = headers.get("authorization");

        if (authHeader == null) {
            return false;
        }

        if (!authHeader.startsWith("Bearer ")) {
            return false;
        }

        authHeader = authHeader.replace("Bearer ", "").trim();
        return Role.Admin == KeyUtil.getKeyRole(authHeader);
    }

    private void respond(Response response) throws IOException {
        Logger.log("Responding with: " + response.getCode() + " " + response.getText());
        StreamUtil.sendResponse(clientRequest.getClientSocket().getOutputStream(), response);
    }
}
