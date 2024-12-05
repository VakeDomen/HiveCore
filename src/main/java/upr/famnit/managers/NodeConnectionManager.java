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
            try {
                handleRequest(connection.waitForRequest());
            } catch (IOException e) {
                handleRequestException(e);
            }
        }
        Logger.network("Worker thread closing.");
    }

    private void handleRequestException(IOException e) {
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
                    clientRequest.queTime() +
                    "\nRequest proxy time: " +
                    clientRequest.proxyTime() +
                    "\nTotal time: " +
                    clientRequest.totalTime()
            );
        } catch (IOException e) {
            Logger.error("Proxying request failed: " +
                    e.getMessage() +
                    "\nRequest time in que: " +
                    clientRequest.queTime() +
                    "\nRequest proxy time: " +
                    clientRequest.proxyTime() +
                    "\nTotal time: " +
                    clientRequest.totalTime()
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
}
