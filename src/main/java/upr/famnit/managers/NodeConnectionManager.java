package upr.famnit.managers;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Handles the connection from the Node.
 */
public class NodeConnectionManager {
    private ServerSocket nodeServerSocket;
    private Socket nodeSocket;

    public NodeConnectionManager(int port) throws IOException {
        nodeServerSocket = new ServerSocket(port);
    }

    /**
     * Accepts a connection from the Node.
     */
    public void acceptConnection() throws IOException {
        System.out.println("Waiting for worker node to connect...");
        nodeSocket = nodeServerSocket.accept();
        System.out.println("Worker node connected: " + nodeSocket.getInetAddress());
    }

    /**
     * Gets the connected Node socket.
     */
    public Socket getNodeSocket() {
        return nodeSocket;
    }

    /**
     * Closes the server socket and the Node socket.
     */
    public void close() throws IOException {
        if (nodeSocket != null) {
            nodeSocket.close();
        }
        if (nodeServerSocket != null) {
            nodeServerSocket.close();
        }
    }
}
