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
}
