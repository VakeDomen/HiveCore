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
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

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
        Logger.log("Worker node connected: " + connection.getInetAddress() , LogLevel.network);
    }

    @Override
    public void run() {
        Logger.log("Worker thread started.", LogLevel.status);
        try {
            authenticateNode();
        } catch (IOException e) {
            Logger.log("Error authenticating worker node: " + e.getMessage(), LogLevel.error);
        }

        while (connection.isFine()) {
            try {
                handleRequest(connection.waitForRequest());
            } catch (IOException e) {
                handleRequestException(e);
            }
        }
        Logger.log("Worker thread closing.", LogLevel.status);
    }

    private void handleRequestException(IOException e) {
        data.incrementExceptionCount();
        Logger.log("Problem receiving request from worker node. Count: " +
                data.getConnectionExceptionCount() +
                "\nError: " +
                e.getMessage(),
                LogLevel.error
        );

        if (data.getConnectionExceptionCount() >= CONNECTION_EXCEPTION_THRESHOLD) {
            Logger.log("Too many exceptions. Closing connection.", LogLevel.warn);
            connection.close();
        }
    }

    private void authenticateNode() throws IOException {
        try {
            waitForAuthRequestAndValidate();
            Logger.log("Worker authenticated: " + data.getNodeName(), LogLevel.status);
        } catch (IOException e) {
            Logger.log("IOException when authenticating node: " + e.getMessage(), LogLevel.error);
        } catch (InterruptedException e) {
            Logger.log("Authenticating node interrupted: " + e.getMessage(), LogLevel.error);
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
        Logger.log("Pulled task. Task waited: " + (System.currentTimeMillis() - clientRequest.getQueEnterTime()) + "ms", LogLevel.status);

        try {
            connection.proxyRequestToNode(clientRequest);
        } catch (IOException e) {
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
}
