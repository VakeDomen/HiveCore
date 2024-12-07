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

        while (connection.isFine()) {
            Request request = null;

            try {
                request = connection.waitForRequest();
            } catch (IOException e) {
                handleRequestException(request, e);
            }

            try {
                handleRequest(request);
            } catch (IOException e) {
                handleHandlingException(request, e);
            }
        }
        Logger.network("Worker thread closing.");
    }

    private void handleHandlingException(Request request, IOException e) {
        data.incrementExceptionCount();
        Logger.error("Problem handling request from worker node. Count: " +
                data.getConnectionExceptionCount() +
                "\nError: " +
                e.getMessage()
        );

        if (data.getConnectionExceptionCount() >= CONNECTION_EXCEPTION_THRESHOLD) {
            Logger.warn("Too many exceptions. Closing connection.");
            connection.close();
        }
    }

    private void handleRequestException(Request request, IOException e) {
        data.incrementExceptionCount();
        Logger.error("Problem receiving request from worker node. Count: " +
                data.getConnectionExceptionCount() +
                "\nError: " +
                e.getMessage()
        );

        if (data.getConnectionExceptionCount() >= CONNECTION_EXCEPTION_THRESHOLD) {
            Logger.warn("Too many exceptions. Closing connection.");
            connection.close();
        }
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
            connection.close();
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
        handlePing(request);

        data.tagsTestAndSet(request.getUri());
        String[] models = request.getUri().split(";");

        ClientRequest clientRequest = null;
        for (String model : models) {
            clientRequest = RequestQue.getTask(model, data.getNodeName());
            if (clientRequest != null) {
                break;
            }
        }
        if (clientRequest == null) {
            connection.send(RequestFactory.EmptyQueResponse());
            return;
        }

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
        return connection.isFine();
    }

    public void closeConnection() throws IOException {
        connection.close();
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
