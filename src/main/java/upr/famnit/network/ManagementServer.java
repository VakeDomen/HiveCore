package upr.famnit.network;

import upr.famnit.managers.ProxyManager;
import upr.famnit.util.Logger;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static upr.famnit.util.Config.MANAGEMENT_CONNECTION_PORT;

/**
 * The {@code ManagementServer} class manages incoming management connections by listening on a specified port
 * and delegating each connection to a separate handler for processing.
 *
 * <p>This class extends {@link Thread} and utilizes an {@link ExecutorService} to handle multiple management
 * connections concurrently. It continuously listens for new management connections on the configured
 * management connection port and submits each accepted connection to the thread pool for processing by
 * {@link ProxyManager}.</p>
 *
 * <p>Thread safety is ensured through the use of thread pools and proper exception handling, ensuring that
 * the server remains robust and responsive under high-load scenarios.</p>
 *
 * <p>Instances of {@code ManagementServer} are intended to run indefinitely, managing the lifecycle of
 * management connections until the application is terminated.</p>
 *
 * @see ProxyManager
 * @see ExecutorService
 * @see Executors
 */
public class ManagementServer extends Thread {

    /**
     * The {@link ServerSocket} that listens for incoming management connections on the configured port.
     */
    private final ServerSocket serverSocket;

    /**
     * The {@link ExecutorService} responsible for managing a pool of threads that handle management connections.
     *
     * <p>A cached thread pool is used to dynamically allocate threads as needed, allowing for efficient handling
     * of a large number of short-lived management connections.</p>
     */
    private final ExecutorService executorService;

    /**
     * Constructs a new {@code ManagementServer} instance by initializing the server socket and the thread pool.
     *
     * <p>This constructor performs the following actions:
     * <ol>
     *     <li>Initializes the {@link ServerSocket} to listen on the configured {@code MANAGEMENT_CONNECTION_PORT}.</li>
     *     <li>Creates a cached thread pool using {@link Executors#newCachedThreadPool()} to manage management handlers.</li>
     * </ol>
     * </p>
     *
     * @throws IOException if an I/O error occurs when opening the server socket
     */
    public ManagementServer() throws IOException {
        this.serverSocket = new ServerSocket(MANAGEMENT_CONNECTION_PORT);
        this.executorService = Executors.newCachedThreadPool();
    }

    /**
     * The main execution method for the {@code ManagementServer} thread.
     *
     * <p>This method performs the following actions:
     * <ol>
     *     <li>Sets the current thread's name to "ManagementServer" for easier identification in logs.</li>
     *     <li>Logs a message indicating that the management connection server is running and listening on the configured port.</li>
     *     <li>Enters an infinite loop to continuously accept and handle incoming management connections.</li>
     *     <li>For each accepted connection, creates a {@link ProxyManager} and submits it to the thread pool for execution.</li>
     *     <li>Logs any {@link IOException} that occurs during the acceptance of management connections.</li>
     * </ol>
     * </p>
     *
     * <p>The loop runs indefinitely, ensuring that the server remains responsive to incoming management connections.</p>
     */
    @Override
    public void run() {
        Thread.currentThread().setName("ManagementServer");
        Logger.network("Management connection server is running on port " + MANAGEMENT_CONNECTION_PORT + "...");

        while (true) {
            try {
                Socket clientSocket = serverSocket.accept();
                executorService.execute(new ProxyManager(clientSocket));
            } catch (IOException e) {
                Logger.error("Something went wrong accepting management connection: " + e.getMessage());
            }
        }
    }

    /**
     * Shuts down the {@code ManagementServer} by closing the server socket and terminating the thread pool.
     *
     * <p>This method performs the following actions:
     * <ol>
     *     <li>Closes the {@link ServerSocket} to stop accepting new management connections.</li>
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
            Logger.info("Management server socket closed.");
        } catch (IOException e) {
            Logger.error("Error closing management server socket: " + e.getMessage());
        }
        executorService.shutdown();
        Logger.info("Executor service shutdown initiated for ManagementServer.");
    }
}
