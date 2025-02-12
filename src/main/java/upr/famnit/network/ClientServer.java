package upr.famnit.network;

import upr.famnit.managers.connections.Client;
import upr.famnit.util.Logger;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static upr.famnit.util.Config.PROXY_PORT;

/**
 * The {@code ClientServer} class manages incoming client connections by listening on a specified port
 * and delegating each connection to a separate handler for processing.
 *
 * <p>This class extends {@link Thread} and utilizes an {@link ExecutorService} to handle multiple client
 * connections concurrently. It continuously listens for new client connections on the configured proxy
 * port and submits each accepted connection to the thread pool for processing by {@link Client}.</p>
 *
 * <p>Thread safety is ensured through the use of thread pools and proper exception handling, ensuring that
 * the server remains robust and responsive under high-load scenarios.</p>
 *
 * <p>Instances of {@code ClientServer} are intended to run indefinitely, managing the lifecycle of client
 * connections until the application is terminated.</p>
 *
 * @see Client
 * @see ExecutorService
 * @see Executors
 */
public class ClientServer extends Thread {

    /**
     * The {@link ExecutorService} responsible for managing a pool of threads that handle client connections.
     *
     * <p>A cached thread pool is used to dynamically allocate threads as needed, allowing for efficient handling
     * of a large number of short-lived client connections.</p>
     */
    private final ExecutorService requestThreadExecutor;

    /**
     * The {@link ServerSocket} that listens for incoming client connections on the configured proxy port.
     */
    private final ServerSocket serverSocket;

    /**
     * Constructs a new {@code ClientServer} instance by initializing the server socket and the thread pool.
     *
     * <p>This constructor performs the following actions:
     * <ol>
     *     <li>Initializes the {@link ServerSocket} to listen on the configured {@code PROXY_PORT}.</li>
     *     <li>Creates a cached thread pool using {@link Executors#newCachedThreadPool()} to manage client handlers.</li>
     * </ol>
     * </p>
     *
     * @throws IOException if an I/O error occurs when opening the socket
     */
    public ClientServer() throws IOException {
        this.serverSocket = new ServerSocket(PROXY_PORT);
        this.requestThreadExecutor = Executors.newCachedThreadPool();
    }

    /**
     * The main execution method for the {@code ClientServer} thread.
     *
     * <p>This method performs the following actions:
     * <ol>
     *     <li>Sets the current thread's name to "ClientServer" for easier identification in logs.</li>
     *     <li>Logs a message indicating that the proxy server is running and listening on the configured port.</li>
     *     <li>Enters an infinite loop to continuously accept and handle incoming client connections.</li>
     *     <li>For each accepted connection, creates a {@link Client} and submits it to the thread pool for execution.</li>
     *     <li>Logs any {@link IOException} that occurs during the acceptance of client connections.</li>
     * </ol>
     * </p>
     *
     * <p>The loop runs indefinitely, ensuring that the server remains responsive to incoming client connections.</p>
     */
    @Override
    public void run() {
        Thread.currentThread().setName("ClientServer");
        Logger.network("Proxy server is running on port " + PROXY_PORT + "...");

        while (true) {
            try {
                Client connection = new Client(serverSocket);
                requestThreadExecutor.submit(connection);
            } catch (IOException e) {
                Logger.error("Something went wrong accepting client connection: " + e.getMessage());
            }
        }
    }

    /**
     * Shuts down the {@code ClientServer} by closing the server socket and terminating the thread pool.
     *
     * <p>This method performs the following actions:
     * <ol>
     *     <li>Closes the {@link ServerSocket} to stop accepting new client connections.</li>
     *     <li>Initiates an orderly shutdown of the {@link ExecutorService}, allowing existing tasks to complete.</li>
     * </ol>
     * </p>
     *
     * <p>It is recommended to call this method during application shutdown to ensure that all resources are
     * released properly.</p>
     */
    public void shutdown() {
        try {
            serverSocket.close();
            Logger.info("Server socket closed.");
        } catch (IOException e) {
            Logger.error("Error closing server socket: " + e.getMessage());
        }
        requestThreadExecutor.shutdown();
        Logger.info("Executor service shutdown initiated.");
    }
}

