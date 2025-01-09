package upr.famnit.network;

import upr.famnit.managers.NodeConnectionManager;
import upr.famnit.managers.NodeConnectionMonitor;
import upr.famnit.util.Logger;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static upr.famnit.util.Config.NODE_CONNECTION_PORT;

/**
 * The {@code NodeServer} class manages incoming worker node connections by listening on a specified port
 * and delegating each connection to a separate handler for processing.
 *
 * <p>This class extends {@link Thread} and utilizes an {@link ExecutorService} to handle multiple worker
 * node connections concurrently. It continuously listens for new worker connections on the configured
 * node connection port and submits each accepted connection to the thread pool for processing by
 * {@link NodeConnectionManager}. Additionally, it monitors active node connections using {@link NodeConnectionMonitor}.</p>
 *
 * <p>Thread safety is ensured through the use of thread pools and proper exception handling, ensuring that
 * the server remains robust and responsive under high-load scenarios.</p>
 *
 * <p>Instances of {@code NodeServer} are intended to run indefinitely, managing the lifecycle of worker
 * node connections until the application is terminated.</p>
 *
 * @see NodeConnectionManager
 * @see NodeConnectionMonitor
 * @see ExecutorService
 * @see Executors
 */
public class NodeServer extends Thread {

    /**
     * The {@link ExecutorService} responsible for managing a pool of threads that handle worker node connections.
     *
     * <p>A cached thread pool is used to dynamically allocate threads as needed, allowing for efficient handling
     * of a large number of short-lived worker connections.</p>
     */
    private final ExecutorService workerPool;

    /**
     * The {@link ServerSocket} that listens for incoming worker node connections on the configured port.
     */
    private final ServerSocket serverSocket;

    /**
     * The {@link NodeConnectionMonitor} that tracks active worker node connections.
     */
    private final NodeConnectionMonitor monitor;

    /**
     * Constructs a new {@code NodeServer} instance by initializing the server socket, connection monitor,
     * and the thread pool.
     *
     * <p>This constructor performs the following actions:
     * <ol>
     *     <li>Initializes the {@link ServerSocket} to listen on the configured {@code NODE_CONNECTION_PORT}.</li>
     *     <li>Creates a new instance of {@link NodeConnectionMonitor} to monitor active connections.</li>
     *     <li>Creates a cached thread pool using {@link Executors#newCachedThreadPool()} to manage worker handlers.</li>
     * </ol>
     * </p>
     *
     * @throws IOException if an I/O error occurs when opening the server socket
     */
    public NodeServer() throws IOException {
        this.serverSocket = new ServerSocket(NODE_CONNECTION_PORT);
        this.monitor = new NodeConnectionMonitor();
        this.workerPool = Executors.newCachedThreadPool();
    }

    /**
     * The main execution method for the {@code NodeServer} thread.
     *
     * <p>This method performs the following actions:
     * <ol>
     *     <li>Sets the current thread's name to "WorkerServer" for easier identification in logs.</li>
     *     <li>Starts the {@link NodeConnectionMonitor} to begin tracking active worker connections.</li>
     *     <li>Logs a message indicating that the worker connection server is running and listening on the configured port.</li>
     *     <li>Enters an infinite loop to continuously accept and handle incoming worker node connections.</li>
     *     <li>For each accepted connection, creates a {@link NodeConnectionManager}, submits it to the thread pool for execution, and adds it to the monitor.</li>
     *     <li>Logs any {@link IOException} that occurs during the acceptance of worker connections.</li>
     * </ol>
     * </p>
     *
     * <p>The loop runs indefinitely, ensuring that the server remains responsive to incoming worker node connections.</p>
     */
    @Override
    public void run() {
        Thread.currentThread().setName("WorkerServer");
        this.monitor.start();
        Logger.network("Worker connection server is running on port " + NODE_CONNECTION_PORT + "...");

        while (true) {
            try {
                NodeConnectionManager nodeConnection = new NodeConnectionManager(serverSocket);
                workerPool.submit(nodeConnection);
                this.monitor.addNode(nodeConnection);
            } catch (IOException e) {
                Logger.error("Something went wrong accepting worker connection: " + e.getMessage());
            }
        }
    }

    /**
     * Shuts down the {@code NodeServer} by closing the server socket, terminating the thread pool,
     * and stopping the connection monitor.
     *
     * <p>This method performs the following actions:
     * <ol>
     *     <li>Closes the {@link ServerSocket} to stop accepting new worker connections.</li>
     *     <li>Initiates an orderly shutdown of the {@link ExecutorService}, allowing existing tasks to complete.</li>
     *     <li>Stops the {@link NodeConnectionMonitor} to cease tracking active connections.</li>
     * </ol>
     * </p>
     *
     * <p>It is recommended to call this method during application shutdown to ensure that all resources are
     * released properly and that no new connections are accepted.</p>
     */
    public void shutdown() {
        try {
            serverSocket.close();
            Logger.info("Worker server socket closed.");
        } catch (IOException e) {
            Logger.error("Error closing worker server socket: " + e.getMessage());
        }
        workerPool.shutdown();
        Logger.info("Worker pool shutdown initiated.");
        monitor.stopMonitoring();
        Logger.info("NodeConnectionMonitor stopped.");
    }
}
