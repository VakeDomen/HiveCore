package upr.famnit.components;

import upr.famnit.util.Logger;
import upr.famnit.util.StreamUtil;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

/**
 * The {@code Connection} class manages the communication between the server and a node via a socket.
 *
 * <p>This class encapsulates a socket connection to a node, handling the sending and receiving of
 * {@link Request} and {@link Response} objects. It provides methods to wait for incoming requests,
 * send responses, proxy client requests to the node, and manage the lifecycle of the connection.</p>
 *
 * <p>Thread safety is ensured through the use of synchronized blocks on dedicated lock objects,
 * {@code socketLock} and {@code stateLock}, which prevent concurrent modifications and access
 * to critical sections of the code.</p>
 *
 * <p>Instances of {@code Connection} are mutable, managing the state of the connection and the
 * associated input/output streams.</p>
 *
 * @see Socket
 * @see Request
 * @see Response
 * @see ClientRequest
 */
public class Connection {

    /**
     * Lock object for synchronizing access to socket operations.
     *
     * <p>Used to ensure that only one thread can perform socket I/O operations at a time.</p>
     */
    private final Object socketLock = new Object();

    /**
     * Lock object for synchronizing access to the connection's state.
     *
     * <p>Used to manage the state of the connection, such as whether it is open or closed.</p>
     */
    private final Object stateLock = new Object();

    /**
     * The socket representing the connection to the node.
     */
    private final Socket nodeSocket;

    /**
     * The input stream for reading data from the node.
     */
    private final InputStream streamFromNode;

    /**
     * The output stream for sending data to the node.
     */
    private final OutputStream streamToNode;

    /**
     * Indicates whether the connection is currently open.
     *
     * <p>This flag is used to manage the lifecycle of the connection, ensuring that operations
     * are only performed on open connections.</p>
     */
    private boolean connectionOpen;

    /**
     * Constructs a new {@code Connection} instance using the provided socket.
     *
     * <p>Initializes the input and output streams associated with the socket and marks the
     * connection as open.</p>
     *
     * @param socket the {@link Socket} connected to the node
     * @throws IOException if an I/O error occurs when creating the input/output streams
     */
    public Connection(Socket socket) throws IOException {
        nodeSocket = socket;
        streamFromNode = nodeSocket.getInputStream();
        streamToNode = nodeSocket.getOutputStream();
        connectionOpen = true;
    }

    /**
     * Checks whether the connection is in a healthy state.
     *
     * <p>This method verifies that the connection is marked as open and that the socket
     * is still connected.</p>
     *
     * @return {@code true} if the connection is open and the socket is connected; {@code false} otherwise
     */
    public boolean isFine() {
        synchronized (stateLock) {
            return connectionOpen && nodeSocket.isConnected();
        }
    }

    /**
     * Retrieves the internet address of the connected node.
     *
     * <p>This method returns the string representation of the node's {@link java.net.InetAddress}.</p>
     *
     * @return the internet address of the node as a {@code String}
     */
    public String getInetAddress() {
        synchronized (stateLock) {
            return nodeSocket.getInetAddress().toString();
        }
    }

    /**
     * Waits for and retrieves the next {@link Request} from the node.
     *
     * <p>This method blocks until a new request is received from the node, then parses
     * it into a {@code Request} object.</p>
     *
     * @return the next {@link Request} received from the node
     * @throws IOException if an I/O error occurs while reading the request
     */
    public Request waitForRequest() throws IOException {
        synchronized (socketLock) {
            return new Request(nodeSocket);
        }
    }

    /**
     * Closes the connection to the node gracefully.
     *
     * <p>This method closes the input and output streams, marks the connection as closed,
     * and closes the socket. It ensures that these operations are performed atomically
     * to prevent inconsistent states.</p>
     *
     * @return {@code true} if the connection was successfully closed; {@code false} otherwise
     */
    public boolean close() {
        synchronized (socketLock) {
            synchronized (stateLock) {
                try {
                    connectionOpen = false;
                    streamFromNode.close();
                    streamToNode.close();
                    nodeSocket.close();
                    return true;
                } catch (IOException e) {
                    Logger.error("Failed to close connection: " + e.getMessage());
                    return false;
                }
            }
        }
    }

    /**
     * Sends a {@link Request} to the node.
     *
     * <p>This method serializes the provided {@code Request} object and sends it over
     * the output stream to the node.</p>
     *
     * @param request the {@link Request} to be sent to the node
     * @throws IOException if an I/O error occurs while sending the request
     */
    public void send(Request request) throws IOException {
        synchronized (socketLock) {
            StreamUtil.sendRequest(streamToNode, request);
        }
    }

    /**
     * Proxies a {@link ClientRequest} to the node and forwards the response back to the client.
     *
     * <p>This method performs the following steps:
     * <ol>
     *     <li>Sends the client's request to the node.</li>
     *     <li>Receives the node's response.</li>
     *     <li>Forwards the response back to the client, handling both standard and chunked transfers.</li>
     *     <li>Updates the client's request state to indicate completion.</li>
     * </ol>
     * </p>
     *
     * <p>It also logs a warning if the node's response status code is not {@code 200 OK}.</p>
     *
     * @param clientRequest the {@link ClientRequest} containing the client's request and connection details
     * @throws IOException if an I/O error occurs during the proxying process
     */
    public void proxyRequestToNode(ClientRequest clientRequest) throws IOException {
        synchronized (socketLock) {
            OutputStream streamToClient = clientRequest.getClientSocket().getOutputStream();

            // Forward the client's request to the node
            StreamUtil.sendRequest(streamToNode, clientRequest.getRequest());
            // Logger.log("Request forwarded to Node.", LogLevel.network);

            // Read the status line from the node's response
            String statusLine = StreamUtil.readLine(streamFromNode);
            if (statusLine == null || statusLine.isEmpty()) {
                throw new IOException("Failed to read status line from node");
            }
            String[] statusLineTokens = statusLine.split(" ", 3);
            if (statusLineTokens.length == 3 && !statusLineTokens[1].equals("200")) {
                Logger.warn("Response not 200: " + statusLine);
                clientRequest.getRequest().log();
            }

            // Forward the status line to the client
            streamToClient.write((statusLine + "\r\n").getBytes(StandardCharsets.US_ASCII));

            // Read the response headers from the node
            Map<String, String> responseHeaders = StreamUtil.readHeaders(streamFromNode);
            // Logger.log("Response headers: " + responseHeaders);

            // Forward headers to the client
            for (Map.Entry<String, String> header : responseHeaders.entrySet()) {
                String headerLine = header.getKey() + ": " + header.getValue();
                streamToClient.write((headerLine + "\r\n").getBytes(StandardCharsets.US_ASCII));
            }
            streamToClient.write("\r\n".getBytes(StandardCharsets.US_ASCII));
            streamToClient.flush();

            // Determine how to handle the response body based on headers
            if (responseHeaders.containsKey("transfer-encoding") &&
                    responseHeaders.get("transfer-encoding").equalsIgnoreCase("chunked")) {
                StreamUtil.readAndForwardChunkedBody(streamFromNode, streamToClient);
            } else if (responseHeaders.containsKey("content-length")) {
                int contentLength = Integer.parseInt(responseHeaders.get("content-length"));
                StreamUtil.readAndForwardFixedLengthBody(streamFromNode, streamToClient, contentLength);
            } else {
                StreamUtil.readAndForwardUntilEOF(streamFromNode, streamToClient);
            }

            // Mark the client's request as complete
            clientRequest.stampResponseFinish();
        }
    }
}
