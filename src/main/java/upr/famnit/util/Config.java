package upr.famnit.util;

import org.ini4j.Wini;

import java.io.File;
import java.io.IOException;

public class Config {
    // boolean determines whether the proxy accepts unauthenticated
    // requests. If set to true, the client must send an
    // Authorization header in the request with a valid
    // Bearer <token>. Valid tokens are provided by the
    // administrator. If set to false, the api is open for use
    // without valid authentication.
    public static boolean USER_AUTHENTICATION = false;

    // Port for client connections
    public static int PROXY_PORT = 6666;

    // Port for Node connection
    public static int NODE_CONNECTION_PORT = 7777;

    // Port where connection for managing proxy is hosted
    public static int MANAGEMENT_CONNECTION_PORT = 6668;

    // time for the message pipeline to complete before
    // timeout is sent to the client
    public static int PROXY_TIMEOUT_MS = 60_000;

    // buffer size for parsing response chunks received from nodes
    // should be enough...
    public static int MESSAGE_CHUNK_BUFFER_SIZE = 16384;

    // number of seconds to wait before killing connection to worker node
    // the seconds between last message received from the worker node
    // if there is no valid request completed in time specified, the connection
    // will be closed
    // applies for nodes that are in POLLING status (worker not polling for requests)
    public static int POLLING_NODE_CONNECTION_TIMEOUT = 10;

    // number of seconds to wait before killing connection to worker node
    // the seconds between last message received from the worker node
    // if there is no valid request completed in time specified, the connection
    // will be closed
    // applies for nodes that are in WORKING status (node request timeouts)
    public static int WORKING_NODE_CONNECTION_TIMEOUT = 300;

    // number of allowed exceptions when communicating to an unstable worker node
    // if the threshold is reached the connection is terminated
    public static int CONNECTION_EXCEPTION_THRESHOLD = 5;

    // sqlite database url
    // used to store access keys
    public static String DATABASE_URL = "jdbc:sqlite:sqlite.db";


    /**
     * Initializes the configuration by loading from config.ini if it exists,
     * otherwise creates config.ini with default values.
     *
     * @throws IOException if an I/O error occurs
     */
    public static void init() throws IOException {
        String CONFIG_FILE = "config.ini";
        File file = new File(CONFIG_FILE);
        if (file.exists()) {
            loadConfig(file);
        } else {
            createDefaultConfig(file);
        }
    }

    /**
     * Loads configuration from the given INI file.
     *
     * @param file the INI file to load
     * @throws IOException if an I/O error occurs
     */
    private static void loadConfig(File file) throws IOException {
        Wini ini = new Wini(file);

        // Load [Server] section
        if (ini.containsKey("Server")) {
            USER_AUTHENTICATION = ini.get("Server", "USER_AUTHENTICATION", Boolean.class);
            PROXY_PORT = ini.get("Server", "PROXY_PORT", Integer.class);
            NODE_CONNECTION_PORT = ini.get("Server", "NODE_CONNECTION_PORT", Integer.class);
            MANAGEMENT_CONNECTION_PORT = ini.get("Server", "MANAGEMENT_CONNECTION_PORT", Integer.class);
        }

        // Load [Connection] section
        if (ini.containsKey("Connection")) {
            CONNECTION_EXCEPTION_THRESHOLD = ini.get("Connection", "CONNECTION_EXCEPTION_THRESHOLD", Integer.class);
            POLLING_NODE_CONNECTION_TIMEOUT = ini.get("Connection", "POLLING_NODE_CONNECTION_TIMEOUT", Integer.class);
            WORKING_NODE_CONNECTION_TIMEOUT = ini.get("Connection", "WORKING_NODE_CONNECTION_TIMEOUT", Integer.class);
            PROXY_TIMEOUT_MS = ini.get("Connection", "PROXY_TIMEOUT_MS", Integer.class);
            MESSAGE_CHUNK_BUFFER_SIZE = ini.get("Connection", "MESSAGE_CHUNK_BUFFER_SIZE", Integer.class);
        }

        // Load [Database] section
        if (ini.containsKey("Database")) {
            DATABASE_URL = ini.get("Database", "DATABASE_URL", String.class);
        }

    }

    /**
     * Creates a default INI configuration file with current default values.
     *
     * @param file the INI file to create
     * @throws IOException if an I/O error occurs
     */
    private static void createDefaultConfig(File file) throws IOException {
        Wini ini = new Wini();

        // [Server] section
        ini.add("Server");
        ini.put("Server", "USER_AUTHENTICATION", USER_AUTHENTICATION);
        ini.put("Server", "PROXY_PORT", PROXY_PORT);
        ini.put("Server", "NODE_CONNECTION_PORT", NODE_CONNECTION_PORT);
        ini.put("Server", "MANAGEMENT_CONNECTION_PORT", MANAGEMENT_CONNECTION_PORT);


        // [Connection] section
        ini.add("Connection");
        ini.put("Connection", "POLLING_NODE_CONNECTION_TIMEOUT", POLLING_NODE_CONNECTION_TIMEOUT);
        ini.put("Connection", "WORKING_NODE_CONNECTION_TIMEOUT", WORKING_NODE_CONNECTION_TIMEOUT);
        ini.put("Connection", "CONNECTION_EXCEPTION_THRESHOLD", CONNECTION_EXCEPTION_THRESHOLD);
        ini.put("Connection", "PROXY_TIMEOUT_MS", PROXY_TIMEOUT_MS);
        ini.put("Connection", "MESSAGE_CHUNK_BUFFER_SIZE", MESSAGE_CHUNK_BUFFER_SIZE);

        // [Database] section
        ini.add("Database");
        ini.put("Database", "DATABASE_URL", DATABASE_URL);

        ini.store(file);
    }
}
