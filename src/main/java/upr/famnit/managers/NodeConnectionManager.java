package upr.famnit.managers;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Handles the connection from the Rust Node.
 */
public class NodeConnectionManager {
    private ServerSocket rustNodeServerSocket;
    private Socket rustNodeSocket;

    public NodeConnectionManager(int port) throws IOException {
        rustNodeServerSocket = new ServerSocket(port);
    }

    /**
     * Accepts a connection from the Rust Node.
     */
    public void acceptConnection() throws IOException {
        System.out.println("Waiting for Rust node to connect...");
        rustNodeSocket = rustNodeServerSocket.accept();
        System.out.println("Rust node connected: " + rustNodeSocket.getInetAddress());
    }

    /**
     * Gets the connected Rust Node socket.
     */
    public Socket getRustNodeSocket() {
        return rustNodeSocket;
    }

    /**
     * Closes the server socket and the Rust Node socket.
     */
    public void close() throws IOException {
        if (rustNodeSocket != null) {
            rustNodeSocket.close();
        }
        if (rustNodeServerSocket != null) {
            rustNodeServerSocket.close();
        }
    }
}
