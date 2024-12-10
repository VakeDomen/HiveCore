package upr.famnit.util;

public class Config {

    // Port for client connections
    public static final int PROXY_PORT = 6666;

    // Port for Node connection
    public static final int NODE_CONNECTION_PORT = 7777;

    // Port where connection for managing proxy is hosted
    public static final int MANAGEMENT_CONNECTION_PORT = 6668;

    // time for the message pipeline to complete before
    // timeout is sent to the client
    public static final int PROXY_TIMEOUT_MS = 60_000;

    // buffer size for parsing response chunks received from nodes
    // should be enough...
    public static final int MESSAGE_CHUNK_BUFFER_SIZE = 16384;

    // number of seconds to wait before killing connection to worker node
    // the seconds between last message received from the worker node
    // if there is no valid request completed in time specified, the connection
    // will be closed
    // applies for nodes that are in POLLING status (worker not polling for requests)
    public static final int POLLING_NODE_CONNECTION_TIMEOUT = 10;

    // number of seconds to wait before killing connection to worker node
    // the seconds between last message received from the worker node
    // if there is no valid request completed in time specified, the connection
    // will be closed
    // applies for nodes that are in WORKING status (node request timeouts)
    public static final int WORKING_NODE_CONNECTION_TIMEOUT = 300;

    // number of allowed exceptions when communicating to an unstable worker node
    // if the threshold is reached the connection is terminated
    public static final int CONNECTION_EXCEPTION_THRESHOLD = 5;

    // sqlite database url
    // used to store access keys
    public static final String DATABASE_URL = "jdbc:sqlite:sqlite.db";

}
