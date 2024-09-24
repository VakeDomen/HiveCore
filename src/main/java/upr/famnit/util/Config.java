package upr.famnit.util;

public class Config {

    // Port for client connections
    public static final int PROXY_PORT = 6666;

    // Port for Node connection
    public static final int NODE_CONNECTION_PORT = 7777;

    // time for the message pipeline to complete before
    // timeout is sent to the client
    public static final int PROXY_TIMEOUT_MS = 30_000;

    // buffer size for parsing response chunks received from nodes
    // should be enough...
    public static final int MESSAGE_CHUNK_BUFFER_SIZE = 8192;

    // number of seconds to wait before killing connection to worker node
    // the seconds between last message received from the worker node
    // if there is no valid request completed in time specified, the connection
    // will be closed
    public static final int NODE_CONNECTION_TIMEOUT = 300;

    // number of milliseconds to sleep when there is an error with communicating
    // with the worker node. Gives the worker time to error-correct before trying again
    public static final int WORKER_GRACE_TIME = 1000;
}
