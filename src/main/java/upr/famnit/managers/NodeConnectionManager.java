package upr.famnit.managers;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Handles the connection from the Node.
 */
public class NodeConnectionManager {

    private Socket nodeSocket;

    /**
     * Accepts a connection from the Node.
     */
    public void acceptConnection(ServerSocket nodeServerSocket) throws IOException {
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
}
