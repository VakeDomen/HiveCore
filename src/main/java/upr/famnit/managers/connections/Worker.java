package upr.famnit.managers.connections;

import upr.famnit.authentication.KeyUtil;
import upr.famnit.authentication.VerificationStatus;
import upr.famnit.authentication.VerificationType;
import upr.famnit.components.*;
import upr.famnit.util.Logger;
import upr.famnit.util.StreamUtil;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * The {@code NodeConnectionManager} class handles connections from worker nodes,
 * managing authentication, request handling, and maintaining the connection state.
 *
 * <p>This class extends {@link Thread} and is responsible for:
 * <ul>
 *     <li>Authenticating worker nodes upon connection.</li>
 *     <li>Managing the lifecycle of the connection, including handling requests and responses.</li>
 *     <li>Interacting with the {@link RequestQue} to retrieve and assign client requests to worker nodes.</li>
 *     <li>Maintaining node-specific data using {@link NodeData}, such as node name, tags, and status.</li>
 * </ul>
 * </p>
 *
 * <p>Thread safety is managed through synchronized methods and careful handling of shared resources.
 * The class ensures that exceptions are properly caught and logged, maintaining the robustness of the system.</p>
 *
 * <p>Instances of {@code NodeConnectionManager} are intended to run in their own threads, handling
 * a single node connection from authentication through to disconnection.</p>
 *
 * @see Connection
 * @see NodeData
 * @see Request
 * @see ClientRequest
 * @see RequestQue
 * @see VerificationStatus
 * @see VerificationType
 */
public class Worker extends Thread {

    /**
     * The {@link Connection} object representing the communication link with the worker node.
     */
    private final Connection connection;

    /**
     * The {@link NodeData} object storing data and status information about the connected node.
     */
    private final NodeData data;

    /**
     * Constructs a new {@code NodeConnectionManager} by accepting a connection from the given server socket.
     *
     * <p>This constructor performs the following actions:
     * <ol>
     *     <li>Accepts an incoming connection from the worker node via the provided {@link ServerSocket}.</li>
     *     <li>Initializes the {@link Connection} object for communication.</li>
     *     <li>Creates a new {@link NodeData} instance to store node-specific data.</li>
     *     <li>Logs the connection establishment.</li>
     * </ol>
     * </p>
     *
     * @param nodeServerSocket the {@link ServerSocket} listening for node connections
     * @throws IOException if an I/O error occurs when accepting the connection
     */
    public Worker(ServerSocket nodeServerSocket) throws IOException {
        Socket socket = nodeServerSocket.accept();
        connection = new Connection(socket);
        data = new NodeData();
        Logger.network("Worker node connected: " + connection.getInetAddress());
    }

    /**
     * The main execution method for the {@code NodeConnectionManager} thread.
     *
     * <p>This method performs the following actions:
     * <ol>
     *     <li>Authenticates the connected worker node.</li>
     *     <li>Enters a loop to handle incoming requests from the node.</li>
     *     <li>Processes each request based on its type (e.g., POLL, PING).</li>
     *     <li>Manages exceptions by logging errors and closing the connection if necessary.</li>
     * </ol>
     * </p>
     */
    @Override
    public void run() {
        Logger.network("Waiting for worker to authenticate...");
        try {
            authenticateNode();
        } catch (IOException e) {
            Logger.error("Error authenticating worker node: " + e.getMessage());
            closeConnection();
            return;
        }

        while (isConnectionOpen()) {
            Request request;

            try {
                request = connection.waitForRequest();
            } catch (IOException e) {
                handleRequestException(null, e);
                continue;
            }

            try {
                handleRequest(request);
            } catch (IOException e) {
                handleHandlingException(request, e);
            }
        }
        Logger.network("Worker thread closing.");
    }

    /**
     * Handles exceptions that occur during the processing of a request.
     *
     * <p>This method logs the error and increments the exception count for the node.
     * If the exception threshold is exceeded, it closes the connection.</p>
     *
     * @param request the {@link Request} that was being processed when the exception occurred
     * @param e       the {@link IOException} that was thrown
     */
    private void handleHandlingException(Request request, IOException e) {
        Logger.error("Problem handling request from worker node. \nError: " +
                e.getMessage()
        );
    }

    /**
     * Handles exceptions that occur while receiving a request from the worker node.
     *
     * <p>This method logs the error and closes the connection, as it indicates a protocol violation or disconnection.</p>
     *
     * @param request the {@link Request} that was being received when the exception occurred (maybe {@code null})
     * @param e       the {@link IOException} that was thrown
     */
    private void handleRequestException(Request request, IOException e) {
        Logger.error("Problem receiving request from worker node. Protocol violation. Error: " + e.getMessage());
        closeConnection();
    }

    /**
     * Authenticates the connected worker node by processing the initial authentication request.
     *
     * <p>This method performs the following actions:
     * <ol>
     *     <li>Waits for the authentication request from the node.</li>
     *     <li>Validates the authentication data (key, nonce, versions).</li>
     *     <li>Sets the node's verification status and name upon successful authentication.</li>
     *     <li>Sends an authentication response back to the node.</li>
     * </ol>
     * </p>
     *
     * @throws IOException if an error occurs during authentication or communication
     */
    private void authenticateNode() throws IOException {
        try {
            waitForAuthRequestAndValidate();
            Logger.status("Worker authenticated: " + data.getNodeName());
            connection.send(RequestFactory.AuthenticationResponse(data.getNodeName()));

        } catch (IOException e) {
            data.setVerificationStatus(VerificationStatus.Rejected);
            Logger.error("IOException when authenticating node: " + e.getMessage());
            throw e;

        } catch (InterruptedException e) {
            data.setVerificationStatus(VerificationStatus.Rejected);
            Logger.error("Authenticating node interrupted: " + e.getMessage());
            Thread.currentThread().interrupt();
            throw new IOException("Authentication interrupted", e);

        } catch (Exception e) {
            data.setVerificationStatus(VerificationStatus.Rejected);
            throw new IOException("Unknown authentication exception: " + e.getMessage());
        }

        if (data.getVerificationStatus() != VerificationStatus.Verified) {
            data.setVerificationStatus(VerificationStatus.Rejected);
            throw new IOException("Authentication not verified: " + data.getVerificationStatus());
        }
    }

    /**
     * Waits for the authentication request from the node and validates the authentication data.
     *
     * <p>This method performs the following validations:
     * <ul>
     *     <li>Checks that the first message is an authentication request with the correct protocol and method.</li>
     *     <li>Parses and validates the authentication key and nonce.</li>
     *     <li>Retrieves node-specific data such as name, version, and Ollama version.</li>
     *     <li>Sets the node's verification status to {@code Waiting} and updates the thread name.</li>
     * </ul>
     * </p>
     *
     * @throws IOException          if the authentication data is invalid
     * @throws InterruptedException if the thread is interrupted while waiting
     */
    private void waitForAuthRequestAndValidate() throws IOException, InterruptedException {
        Request request = connection.waitForRequest();
        Logger.log("Recieved message...");

        if (!"HIVE".equals(request.getProtocol()) || !"AUTH".equals(request.getMethod())) {
            throw new IOException("First message should be authentication");
        }
        Logger.log("Recieved auth message. Verifying...");

        String[] authenticationData = request.getUri().split(";");
        if (authenticationData.length != 4) {
            throw new IOException("Invalid authentication data format.");
        }

        String key = authenticationData[0];
        String nonce = authenticationData[1];
        String hiveVersion = authenticationData[2];
        String ollamaVersion = authenticationData[3];

        if (!KeyUtil.verifyKey(key, VerificationType.NodeConnection)) {
            throw new IOException("Invalid authentication key.");
        }

        data.setNonce(nonce);
        data.setNodeName(KeyUtil.nameKey(key));
        data.setVerificationStatus(VerificationStatus.Waiting);
        data.setNodeVersion(new WorkerVersion(hiveVersion, ollamaVersion));
        Thread.currentThread().setName(data.getNodeName());


        while (data.getVerificationStatus() == VerificationStatus.Waiting) {
            Thread.sleep(50);
        }
    }

    /**
     * Handles incoming requests from the worker node based on their method and protocol.
     *
     * <p>This method delegates the request handling to specific methods based on the request type,
     * such as handling polling requests or ping requests.</p>
     *
     * @param request the {@link Request} received from the node
     * @throws IOException if an error occurs during request handling
     */
    private void handleRequest(Request request) throws IOException {
        if ("HIVE".equals(request.getProtocol())) {
            switch (request.getMethod()) {
                case "POLL":
                    handlePollRequest(request);
                    break;
                case "PING":
                    handlePing(request);
                    break;
                default:
                    handlePing(request);
                    break;
            }
        }
    }

    /**
     * Handles a ping request from the worker node to update its last active time.
     *
     * @param request the {@link Request} received from the node
     */
    private void handlePing(Request request) {
        data.setLastPing(LocalDateTime.now());
    }

    /**
     * Handles a polling request from the worker node to assign client requests for processing.
     *
     * <p>This method performs the following actions:
     * <ol>
     *     <li>Updates the node's verification status to {@code Polling} and updates the last ping time.</li>
     *     <li>Retrieves a client request from the queue based on the node's capabilities or models.</li>
     *     <li>If a request is available, proxies the request to the node and handles the response back to the client.</li>
     *     <li>Updates the node's verification status accordingly.</li>
     * </ol>
     * </p>
     *
     * @param request the {@link Request} received from the node
     * @throws IOException if an error occurs during communication with the node or client
     */
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
            Logger.success("Request handled by: " + data.getNodeName() +
                    "\nRequest time in queue: " + String.format("%,d", clientRequest.queTime()) + " ms" +
                    "\nRequest proxy time: " + String.format("%,d", clientRequest.proxyTime()) + " ms" +
                    "\nTotal time: " + String.format("%,d", clientRequest.totalTime()) + " ms"
            );
        } catch (IOException e) {
            Logger.error("Proxying request failed: " + e.getMessage() +
                    "\nRequest time in queue: " + String.format("%,d", clientRequest.queTime()) + " ms" +
                    "\nRequest proxy time: " + String.format("%,d", clientRequest.proxyTime()) + " ms" +
                    "\nTotal time: " + String.format("%,d", clientRequest.totalTime()) + " ms"
            );
            StreamUtil.sendResponse(
                    clientRequest.getClientSocket().getOutputStream(),
                    ResponseFactory.BadRequest()
            );
        }
        data.setVerificationStatus(VerificationStatus.CompletedWork);
    }

    /**
     * Retrieves a client request from the queue based on the worker node's capabilities or models.
     *
     * <p>This method decides which polling strategy to use based on the request URI:
     * <ul>
     *     <li>If the URI is "-", it uses sequenced polling to optimize model sequencing.</li>
     *     <li>Otherwise, it uses default polling based on the models specified in the URI.</li>
     * </ul>
     * </p>
     *
     * @param request the polling {@link Request} received from the node
     * @return a {@link ClientRequest} to be processed by the node, or {@code null} if none are available
     */
    private ClientRequest getRequestFromQueue(Request request) {
        if ("-".equals(request.getUri())) {
            return sequencedPolling(request);
        } else {
            return defaultPolling(request);
        }
    }

    /**
     * Performs default polling to retrieve a client request matching the node's capabilities.
     *
     * <p>This method updates the node's tags and searches the request queue for a matching request.</p>
     *
     * @param request the polling {@link Request} received from the node
     * @return a matching {@link ClientRequest}, or {@code null} if none are found
     */
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

    /**
     * Performs sequenced polling to optimize model sequencing and reduce model swaps.
     *
     * <p>This method reorders the node's tags based on the requests it handles to minimize model swapping in memory.</p>
     *
     * @param request the polling {@link Request} received from the node
     * @return a matching {@link ClientRequest}, or {@code null} if none are found
     */
    private ClientRequest sequencedPolling(Request request) {
        String tagsString = data.getTags();
        if (tagsString == null || tagsString.isBlank()) {
            return null;
        }

        String[] tags = tagsString.split(";");
        if (tags.length == 0) {
            return null;
        }

        for (int i = 0; i < tags.length; i++) {
            ClientRequest clientRequest = RequestQue.getTask(tags[i], data.getNodeName());
            if (clientRequest != null) {
                // Rotate the tags array to prioritize the model just used
                if (i > 0) {
                    String[] newTags = new String[tags.length];
                    for (int j = 0; j < tags.length; j++) {
                        newTags[(j - i + tags.length) % tags.length] = tags[j];
                    }
                    data.tagsTestAndSet(String.join(";", newTags));
                }
                return clientRequest;
            }
        }
        return null;
    }

    /**
     * Checks whether the connection to the worker node is still open and valid.
     *
     * @return {@code true} if the connection is open; {@code false} otherwise
     */
    public boolean isConnectionOpen() {
        return data.getVerificationStatus() != VerificationStatus.Closed && connection.isFine();
    }

    /**
     * Closes the connection to the worker node and updates the verification status.
     */
    public void closeConnection() {
        if (connection.close()) {
            data.setVerificationStatus(VerificationStatus.Closed);
            Logger.network("Connection closed for node: " + data.getNodeName());
        }
    }

    /**
     * Retrieves the {@link NodeData} associated with this connection.
     *
     * @return the {@link NodeData} object containing node-specific information
     */
    public NodeData getData() {
        return data;
    }

    /**
     * Retrieves the tags (models) associated with the worker node.
     *
     * <p>This method parses the tags string into a list for easier handling.</p>
     *
     * @return an {@link ArrayList} of tags (models) supported by the node
     */
    public ArrayList<String> getTags() {
        String tagsString = data.getTags();
        if (tagsString == null || tagsString.isBlank() || "/".equals(tagsString)) {
            return new ArrayList<>();
        }
        return new ArrayList<>(List.of(tagsString.split(";")));
    }
}
