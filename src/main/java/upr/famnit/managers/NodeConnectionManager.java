package upr.famnit.managers;

import upr.famnit.authentication.KeyUtil;
import upr.famnit.authentication.VerificationStatus;
import upr.famnit.authentication.VerificationType;
import upr.famnit.components.*;
import upr.famnit.util.Logger;
import upr.famnit.util.StreamUtil;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static upr.famnit.util.Config.CONNECTION_EXCEPTION_THRESHOLD;

/**
 * Handles the connection from the Node.
 */
public class NodeConnectionManager extends Thread {

    private final Connection connection;
    private final NodeData data;

    public NodeConnectionManager(ServerSocket nodeServerSocket) throws IOException {
        Socket socket = nodeServerSocket.accept();
        connection = new Connection(socket);
        data = new NodeData();
        Logger.network("Worker node connected: " + connection.getInetAddress());
    }

    @Override
    public void run() {
        Logger.network("Waiting for worker to authenticate...");
        try {
            authenticateNode();
        } catch (IOException e) {
            Logger.error("Error authenticating worker node: " + e.getMessage());
        }

        while (isConnectionOpen()) {
            Request request = null;

            try {
                request = connection.waitForRequest();
            } catch (IOException e) {
                handleRequestException(request, e);
                continue;
            }

            try {
                handleRequest(request);
                data.resetExceptionCount();
            } catch (IOException e) {
                handleHandlingException(request, e);
            }
        }
        Logger.network("Worker thread closing.");
    }

    private void handleHandlingException(Request request, IOException e) {
        Logger.error("Problem handling request from worker node. \nError: " +
                e.getMessage()
        );
    }

    private void handleRequestException(Request request, IOException e) {
        Logger.error("Problem receiving request from worker node. Protocol violation\nError: " +
                e.getMessage()
        );

        closeConnection();
    }

    private void authenticateNode() throws IOException {
        try {
            waitForAuthRequestAndValidate();
            Logger.status("Worker authenticated: " + data.getNodeName());
        } catch (IOException e) {
            Logger.error("IOException when authenticating node: " + e.getMessage());
        } catch (InterruptedException e) {
            Logger.error("Authenticating node interrupted: " + e.getMessage());
        }

        if (data.getVerificationStatus() != VerificationStatus.Verified) {
            closeConnection();
            return;
        }

        Thread.currentThread().setName(data.getNodeName());
    }

    private void waitForAuthRequestAndValidate() throws IOException, InterruptedException {
        Request request = connection.waitForRequest();
        if (!request.getProtocol().equals("HIVE") || !request.getMethod().equals("AUTH")) {
            throw new IOException("First message should be authentication");
        }

        String[] keyAndNonce = request.getUri().split(";");
        if (keyAndNonce.length != 2) {
            throw new IOException("Not valid authentication key and nonce pair.");
        }

        String key = keyAndNonce[0];
        String nonce = keyAndNonce[1];

        if (!KeyUtil.verifyKey(key, VerificationType.NodeConnection)) {
            throw new IOException("Not valid authentication key.");
        }

        data.setNonce(nonce);
        data.setNodeName(KeyUtil.nameKey(key));
        data.setVerificationStatus(VerificationStatus.Waiting);
        while (data.getVerificationStatus() == VerificationStatus.Waiting) sleep(50);
    }

    private void handleRequest(Request request) throws IOException {
        if (request.getProtocol().equals("HIVE")) {
            switch (request.getMethod()) {
                case "POLL" -> handlePollRequest(request);
                case null, default -> handlePing(request);
            }
        }
    }

    private void handlePing(Request request) {
        data.setLastPing(LocalDateTime.now());
    }

    private void handlePollRequest(Request request) throws IOException {
        data.setVerificationStatus(VerificationStatus.Polling);
        handlePing(request);

        ClientRequest clientRequest = getRequestFromQueue(request);
        if (clientRequest == null) {
            connection.send(RequestFactory.EmptyQueResponse());
            return;
        }

        data.setVerificationStatus(VerificationStatus.Working);
        try {
            connection.proxyRequestToNode(clientRequest);
            Logger.success("Request handled by: " +
                    data.getNodeName() +
                    "\nRequest time in que: " +
                    String.format("%,d", clientRequest.queTime()) +
                    "ms\nRequest proxy time: " +
                    String.format("%,d", clientRequest.proxyTime()) +
                    "ms\nTotal time: " +
                    String.format("%,d", clientRequest.totalTime()) +
                    "ms"
            );
        } catch (IOException e) {
            Logger.error("Proxying request failed: " +
                    e.getMessage() +
                    "\nRequest time in que: " +
                    String.format("%,d", clientRequest.queTime()) +
                    "ms\nRequest proxy time: " +
                    String.format("%,d", clientRequest.proxyTime()) +
                    "ms\nTotal time: " +
                    String.format("%,d", clientRequest.totalTime()) +
                    "ms"
            );
            StreamUtil.sendResponse(
                clientRequest.getClientSocket().getOutputStream(),
                ResponseFactory.BadRequest()
            );
        }
        data.setVerificationStatus(VerificationStatus.CompletedWork);
    }

    private ClientRequest getRequestFromQueue(Request request) {
        ClientRequest work = null;

        // dynamic mode for optimized sequencing of model requests
        // if target is set to "-" we assume the worker has already posted the
        // models available in the tags.
        // The sequence of models in the tags is changed such that the model that
        // was found is the next line for the next request if "-" is used again.
        // meant to lower the amount of model-swaps in the VRAM of the worker nodes
        // [A, B, C, D, E]
        //        ^ i=2
        // index = index - i % len
        //      ˘ ˘
        // [C, D, E, A ,B]
        if (request.getUri().equals("-")) {
            work = sequencedPolling(request);
        } else {
            work = defaultPolling(request);
        }

        return work;
    }

    private ClientRequest defaultPolling(Request request) {
        data.tagsTestAndSet(request.getUri());
        String[] models = request.getUri().split(";");

        for (String model : models) {
            ClientRequest clientRequest = RequestQue.getTask(model, data.getNodeName());
            if (clientRequest != null) {
                return clientRequest;
            }
        }
        return null;
    }

    private ClientRequest sequencedPolling(Request request) {
        String tagsString = data.getTags();
        if (tagsString == null) {
            return null;
        }

        String[] tags = tagsString.split(";");
        if (tags.length == 0) {
            return null;
        }

        for (int i = 0; i < tags.length ; i++) {
            ClientRequest clientRequest = RequestQue.getTask(tags[i], data.getNodeName());
            if (clientRequest != null) {
                String[] newTags = new String[tags.length];
                if (i > 0) {
                    for (int j = 0 ; j < tags.length ; j++) {
                        newTags[(tags.length + j - i) % tags.length] = tags[j];
                    }
                    data.tagsTestAndSet(String.join(";", newTags));
                }
                return clientRequest;
            }
        }
        return null;
    }

    public LocalDateTime getLastPing() {
        return data.getLastPing();
    }

    public VerificationStatus getVerificationStatus() {
        return data.getVerificationStatus();
    }

    public void setVerificationStatus(VerificationStatus status) {
        data.setVerificationStatus(status);
    }

    public String getNodeName() {
        return data.getNodeName();
    }

    public String getNonce() {
        return data.getNonce();
    }

    public boolean isConnectionOpen() {
        return data.getVerificationStatus() != VerificationStatus.Closed && connection.isFine();
    }

    public void closeConnection() {
        if (connection.close()) {
            data.setVerificationStatus(VerificationStatus.Closed);
        }
    }



    public ArrayList<String> getTags() {
        String tagsString = data.getTags();
        if (tagsString == null) {
            return new ArrayList<>();
        }
        if (tagsString.equals("/")) {
            return new ArrayList<>();
        }
        if (tagsString.isBlank()) {
            return new ArrayList<>();
        }
        return new ArrayList<>(List.of(tagsString.split(";")));
    }
}
