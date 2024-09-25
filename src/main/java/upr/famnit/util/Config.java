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

    // number of allowed exceptions when communicating to an unstable worker node
    // if the threshold is reached the connection is terminated
    public static final int CONNECTION_EXCEPTION_THRESHOLD = 5;

    // sqlite database url
    // used to store access keys
    public static final String DATABASE_URL = "jdbc:sqlite:sqlite.db";

}
