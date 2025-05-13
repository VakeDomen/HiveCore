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

import static java.nio.charset.StandardCharsets.US_ASCII;

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
    private final ReentrantLock socketLock = new ReentrantLock();

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
    private volatile boolean connectionOpen;

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
        return connectionOpen && nodeSocket.isConnected();
    }

    /**
     * Retrieves the internet address of the connected node.
     *
     * <p>This method returns the string representation of the node's {@link java.net.InetAddress}.</p>
     *
     * @return the internet address of the node as a {@code String}
     */
    public String getInetAddress() {
        return nodeSocket.getInetAddress().toString();
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
        socketLock.lock();
        try {
            return new Request(nodeSocket);
        } finally {
            socketLock.unlock();
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
        try {
            streamFromNode.close();
            streamToNode.close();
            nodeSocket.close();
            connectionOpen = false;
            return true;

        } catch (IOException e) {
            Logger.error("Failed to close connection: " + e.getMessage());
            return false;
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
        socketLock.lock();
        try {
            StreamUtil.sendRequest(streamToNode, request);

        } finally {
            socketLock.unlock();
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
    public void proxyRequestToNode(ClientRequest clientRequest) {
        OutputStream streamToClient = null;
        boolean headersWritten = false;
        socketLock.lock();
        try {
            streamToClient = clientRequest.getClientSocket().getOutputStream();

            // 1. forward client→node
            StreamUtil.sendRequest(streamToNode, clientRequest.getRequest());

            // 2. read node status line
            String statusLine = StreamUtil.readLine(streamFromNode);
            if (statusLine == null || statusLine.isEmpty()) {
                throw new IOException("Failed to read status line from node");
            }
            String[] tokens = statusLine.split(" ", 3);
            if (tokens.length == 3 && !tokens[1].equals("200")) {
                Logger.warn("Node responded non-200: " + statusLine);
                clientRequest.getRequest().log();
            }

            // 3. write status + headers
            streamToClient.write((statusLine + "\r\n").getBytes(US_ASCII));
            Map<String,String> headers = StreamUtil.readHeaders(streamFromNode);
            for (Map.Entry<String,String> h : headers.entrySet()) {
                streamToClient.write((h.getKey()+": "+h.getValue()+"\r\n")
                        .getBytes(US_ASCII));
            }
            streamToClient.write("\r\n".getBytes(US_ASCII));
            streamToClient.flush();
            headersWritten = true;

            // 4. proxy body
            if ("chunked".equalsIgnoreCase(headers.get("transfer-encoding"))) {
                StreamUtil.readAndForwardChunkedBody(streamFromNode, streamToClient);
            } else if (headers.containsKey("content-length")) {
                int len = Integer.parseInt(headers.get("content-length"));
                StreamUtil.readAndForwardFixedLengthBody(
                        streamFromNode, streamToClient, len);
            } else {
                StreamUtil.readAndForwardUntilEOF(streamFromNode, streamToClient);
            }
        }
        catch (IOException ioe) {
            Logger.error("Proxying request failed: " + ioe.getMessage() +
                    "\nRequest time in queue: " + String.format("%,d", clientRequest.queTime()) + " ms" +
                    "\nRequest proxy time: " + String.format("%,d", clientRequest.proxyTime()) + " ms" +
                    "\nTotal time: " + String.format("%,d", clientRequest.totalTime()) + " ms"
            );
            if (streamToClient != null && !headersWritten) {
                try {
                    // send simple 502 if we failed before writing headers
                    StreamUtil.sendResponse(streamToClient, ResponseFactory.BadGateway());
                } catch (IOException ignored) {
                    Logger.error("Could not reply with Bad Gateway!");
                }
            }
        }
        catch (RuntimeException rte) {
            Logger.error("Proxying request failed: " + rte.getMessage() +
                    "\nRequest time in queue: " + String.format("%,d", clientRequest.queTime()) + " ms" +
                    "\nRequest proxy time: " + String.format("%,d", clientRequest.proxyTime()) + " ms" +
                    "\nTotal time: " + String.format("%,d", clientRequest.totalTime()) + " ms"
            );
            try {
                StreamUtil.sendResponse(streamToClient, ResponseFactory.InternalServerError());
            } catch (IOException e) {
                Logger.error("Could not reply with internal server error!");
            }
        }
        finally {
            socketLock.unlock();
            clientRequest.stampResponseFinish();
        }
    }

}
