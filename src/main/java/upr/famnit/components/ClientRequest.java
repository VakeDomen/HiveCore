package upr.famnit.components;

import upr.famnit.authentication.KeyUtil;
import upr.famnit.authentication.VerificationStatus;
import upr.famnit.authentication.VerificationType;
import upr.famnit.util.Config;
import upr.famnit.util.Logger;
import upr.famnit.util.StreamUtil;

import javax.security.sasl.AuthenticationException;
import java.io.IOException;
import java.net.Socket;
import java.util.Map;

import static upr.famnit.util.Config.PROXY_TIMEOUT_MS;

/**
 * The {@code ClientRequest} class encapsulates a client's request, managing the communication
 * between the client and the node. It handles authorization, tracks request processing times,
 * and facilitates proxying of requests to the node.
 *
 * <p>This class is responsible for:
 * <ul>
 *     <li>Parsing and storing the client's request data.</li>
 *     <li>Authenticating the client's request based on provided credentials.</li>
 *     <li>Tracking various timestamps to measure request processing durations.</li>
 *     <li>Facilitating the forwarding of requests to the node and handling responses.</li>
 * </ul>
 * </p>
 *
 * <p>Instances of {@code ClientRequest} are mutable and designed to be used within a single thread
 * context, ensuring that request-specific data is managed safely and efficiently.</p>
 *
 * @see Socket
 * @see Request
 * @see VerificationStatus
 */
public class ClientRequest {

    /**
     * The socket representing the client's connection.
     */
    private final Socket clientSocket;

    /**
     * The {@link Request} object representing the client's request data.
     */
    private final Request request;

    /**
     * The timestamp when the request entered the processing queue.
     *
     * <p>Measured in milliseconds since the epoch.</p>
     */
    private long queEnterTime;

    /**
     * The timestamp when the request left the processing queue.
     *
     * <p>Measured in milliseconds since the epoch.</p>
     */
    private long queLeftTime;

    /**
     * The timestamp when the response to the request was finished.
     *
     * <p>Measured in milliseconds since the epoch.</p>
     */
    private long responseFinishTime;

    /**
     * The name of the node that processed the request.
     */
    private String responseNodeName;

    /**
     * Constructs a new {@code ClientRequest} instance by initializing the connection and processing the request.
     *
     * <p>This constructor performs the following actions:
     * <ol>
     *     <li>Initializes the client's socket connection.</li>
     *     <li>Sets the socket's timeout based on the configuration.</li>
     *     <li>Parses the incoming request from the client socket.</li>
     *     <li>Performs authorization checks if user authentication is enabled.</li>
     * </ol>
     * </p>
     *
     * @param clientSocket the client's socket connection
     * @throws IOException if an I/O error occurs while reading from the socket
     * @throws AuthenticationException if the authorization process fails
     */
    public ClientRequest(Socket clientSocket) throws IOException, AuthenticationException {
        this.clientSocket = clientSocket;
        this.clientSocket.setSoTimeout(PROXY_TIMEOUT_MS);
        this.request = new Request(clientSocket);
        this.authorize();
    }

    public ClientRequest(Socket clientSocket, Request request) throws IOException {
        this.clientSocket = clientSocket;
        this.clientSocket.setSoTimeout(PROXY_TIMEOUT_MS);
        this.request = request;
    }

    /**
     * Authorizes the client's request based on the provided authorization headers.
     *
     * <p>If user authentication is enabled in the configuration, this method checks for the presence
     * and validity of the "authorization" header. It validates the format and verifies the key using
     * {@link KeyUtil#verifyKey(String, VerificationType)}.</p>
     *
     * @throws AuthenticationException if the authorization header is missing, malformed, or invalid
     */
    private void authorize() throws AuthenticationException {
        if (Config.USER_AUTHENTICATION) {
            Map<String, String> headers = this.request.getHeaders();
            if (!headers.containsKey("authorization")) {
                throw new AuthenticationException("Missing authorization header");
            }

            String[] tokens = headers.get("authorization").split(" ");
            if (tokens.length != 2) {
                throw new AuthenticationException("Invalid authorization header");
            }

            String key = tokens[1];
            if (!KeyUtil.verifyKey(key, VerificationType.ClientRequest)) {
                throw new AuthenticationException("Invalid authorization key");
            }
        }
    }

    /**
     * Retrieves the client's socket connection.
     *
     * @return the {@link Socket} representing the client's connection
     */
    public Socket getClientSocket() {
        return clientSocket;
    }

    /**
     * Retrieves the client's request data.
     *
     * @return the {@link Request} object containing the client's request details
     */
    public Request getRequest() {
        return request;
    }

    /**
     * Records the timestamp when the request enters the processing queue.
     *
     * <p>Sets {@code queEnterTime} to the current system time in milliseconds.</p>
     */
    public void stampQueueEnter() {
        this.queEnterTime = System.currentTimeMillis();
    }

    /**
     * Records the timestamp when the request leaves the processing queue and associates it with a node.
     *
     * <p>Sets {@code queLeftTime} to the current system time in milliseconds and records the name of the node
     * that will process the request.</p>
     *
     * @param responseNodeName the name of the node handling the request
     */
    public void stampQueueLeave(String responseNodeName) {
        this.responseNodeName = responseNodeName;
        this.queLeftTime = System.currentTimeMillis();
    }

    /**
     * Records the timestamp when the response to the request has been fully processed.
     *
     * <p>Sets {@code responseFinishTime} to the current system time in milliseconds.</p>
     */
    public void stampResponseFinish() {
        this.responseFinishTime = System.currentTimeMillis();
    }

    /**
     * Calculates the time the request spent in the processing queue.
     *
     * <p>If the request is still in the queue, it returns the duration from queue entry to the current time.
     * If the request has left the queue, it returns the duration from queue entry to queue leave.</p>
     *
     * @return the queue time in milliseconds
     */
    public long queTime() {
        if (queEnterTime == 0) {
            return 0L;
        }

        if (queLeftTime == 0) {
            return System.currentTimeMillis() - queEnterTime;
        }

        return queLeftTime - queEnterTime;
    }

    /**
     * Calculates the time taken to proxy the request to the node.
     *
     * <p>If the request has not yet left the queue, it returns zero. If the response is still being processed,
     * it returns the duration from queue leave to the current time. Otherwise, it returns the duration from
     * queue leave to response finish.</p>
     *
     * @return the proxy time in milliseconds
     */
    public long proxyTime() {
        if (queLeftTime == 0) {
            return 0L;
        }

        if (responseFinishTime == 0) {
            return System.currentTimeMillis() - queLeftTime;
        }

        return responseFinishTime - queLeftTime;
    }

    /**
     * Calculates the total time taken to process the request from queue entry to response finish.
     *
     * <p>If the request is still in the queue, it returns the duration from queue entry to the current time.
     * If the response is still being processed, it returns the duration from queue entry to response finish.
     * Otherwise, it returns the total duration from queue entry to response finish.</p>
     *
     * @return the total processing time in milliseconds
     */
    public long totalTime() {
        if (queEnterTime == 0) {
            return 0L;
        }

        if (responseFinishTime == 0) {
            return System.currentTimeMillis() - queEnterTime;
        }

        return responseFinishTime - queEnterTime;
    }
}
