package upr.famnit.managers;

import upr.famnit.components.*;
import upr.famnit.util.Logger;
import upr.famnit.util.StreamUtil;

import javax.security.sasl.AuthenticationException;
import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * The {@code ClientConnectionManager} class is responsible for handling incoming client connections.
 *
 * <p>This class listens for incoming client connections on a specified {@link ServerSocket}. Upon accepting
 * a connection, it initializes a {@link ClientRequest} to parse and authorize the client's request. If the
 * request is valid and authorized, it adds the request to the processing queue via {@link RequestQue}.
 * Otherwise, it rejects the request by sending an appropriate {@link Response} back to the client.</p>
 *
 * <p>The class implements the {@link Runnable} interface, allowing instances to be executed by a thread pool
 * or individual threads, facilitating concurrent handling of multiple client connections.</p>
 *
 * <p>Thread safety is managed through synchronized blocks and proper exception handling to ensure that
 * resources are appropriately managed and that the system remains robust against malformed or unauthorized
 * requests.</p>
 *
 * @see ServerSocket
 * @see Socket
 * @see ClientRequest
 * @see RequestQue
 * @see ResponseFactory
 */
public class ClientConnectionManager implements Runnable {

    /**
     * The socket representing the client's connection.
     */
    private final Socket clientSocket;

    /**
     * Constructs a new {@code ClientConnectionManager} by accepting a connection from the given server socket.
     *
     * <p>This constructor blocks until a client connects to the server socket. Once a connection is accepted,
     * it initializes the {@code clientSocket} for further communication.</p>
     *
     * @param clientServer the {@link ServerSocket} to accept incoming client connections
     * @throws IOException if an I/O error occurs when waiting for a connection or accepting it
     */
    public ClientConnectionManager(ServerSocket clientServer) throws IOException {
        this.clientSocket = clientServer.accept();
    }

    /**
     * The entry point for the {@code ClientConnectionManager} thread.
     *
     * <p>This method performs the following actions:
     * <ol>
     *     <li>Attempts to create a {@link ClientRequest} from the accepted client socket.</li>
     *     <li>Handles authentication exceptions by rejecting unauthorized requests.</li>
     *     <li>Adds valid client requests to the {@link RequestQue} for processing.</li>
     *     <li>Handles invalid request structures by sending a "Method Not Allowed" response.</li>
     * </ol>
     * </p>
     */
    @Override
    public void run() {
        ClientRequest cr = null;
        try {
            cr = new ClientRequest(clientSocket);
        } catch (AuthenticationException e) {
            Logger.error("Unauthorized client request (" + clientSocket.getRemoteSocketAddress() + "): " + e.getMessage());
            this.rejectRequest(clientSocket);
            return;
        } catch (IOException e) {
            Logger.error("Failed reading client request (" + clientSocket.getRemoteSocketAddress() + "): " + e.getMessage());
            return;
        }

        if (!RequestQue.addTask(cr)) {
            Logger.error("Closing request due to invalid structure (" + clientSocket.getRemoteSocketAddress() + ")");
            Response failedResponse = ResponseFactory.MethodNotAllowed();
            try {
                StreamUtil.sendResponse(cr.getClientSocket().getOutputStream(), failedResponse);
            } catch (IOException e) {
                Logger.error("Unable to respond to the client (" + clientSocket.getRemoteSocketAddress() + "): " + e.getMessage());
            }
            try {
                cr.getClientSocket().close();
            } catch (IOException e) {
                Logger.error("Unable to close connection to the client (" + clientSocket.getRemoteSocketAddress() + "): " + e.getMessage());
            }
            cr.getRequest().log();
        }
    }

    /**
     * Rejects an unauthorized client request by sending an appropriate response.
     *
     * <p>This method sends an {@link ResponseFactory#Unauthorized()} response to the client socket's
     * output stream. If an I/O error occurs during this process, it logs an error message.</p>
     *
     * @param clientSocket the {@link Socket} representing the client's connection to be rejected
     */
    private void rejectRequest(Socket clientSocket) {
        try {
            OutputStream os = clientSocket.getOutputStream();
            StreamUtil.sendResponse(os, ResponseFactory.Unauthorized());
        } catch (IOException e) {
            Logger.error("Could not send rejection response to socket: " + clientSocket.getInetAddress());
        }
    }
}
