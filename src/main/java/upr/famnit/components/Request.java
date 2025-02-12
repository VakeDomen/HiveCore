package upr.famnit.components;

import upr.famnit.authentication.KeyUtil;
import upr.famnit.authentication.VerificationType;
import upr.famnit.util.Config;
import upr.famnit.util.Logger;
import upr.famnit.util.StreamUtil;

import javax.security.sasl.AuthenticationException;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The {@code Request} class represents an HTTP-like request received from a client.
 *
 * <p>This class encapsulates the details of a client's request, including the protocol,
 * HTTP method, URI, headers, and body. It provides functionality to parse incoming
 * data from a {@link Socket} and expose the request components for further processing
 * by the server.</p>
 *
 * <p>The class is designed to handle both standard HTTP requests and custom protocols,
 * such as "HIVE". For "HIVE" protocol requests, headers and body are not processed.</p>
 *
 * <p>Thread safety is maintained as instances of {@code Request} are immutable once created.</p>
 *
 * @see Socket
 * @see StreamUtil
 */
public class Request {

    /**
     * The protocol used for the request (e.g., "HTTP/1.1", "HIVE").
     */
    private final String protocol;

    /**
     * The HTTP method of the request (e.g., "GET", "POST").
     */
    private final String method;

    /**
     * The URI requested by the client.
     */
    private final String uri;

    /**
     * A map containing the request headers. Each key-value pair represents a header name and its corresponding value.
     */
    private final Map<String, String> headers;

    /**
     * The body of the request as a byte array. It is {@code null} if the request does not contain a body.
     */
    private final byte[] body;

    /**
     * Constructs a new {@code Request} instance with the specified parameters.
     *
     * <p>This constructor is typically used when creating a {@code Request} object
     * from already parsed request data.</p>
     *
     * @param protocol the protocol used for the request
     * @param method the HTTP method of the request
     * @param uri the URI requested by the client
     * @param headers a map of header names and their corresponding values
     * @param body the body of the request as a byte array
     * @throws IllegalArgumentException if {@code protocol}, {@code method}, or {@code uri} is {@code null} or empty
     */
    public Request(String protocol, String method, String uri, Map<String, String> headers, byte[] body) {
        if (protocol == null || protocol.trim().isEmpty()) {
            throw new IllegalArgumentException("Protocol cannot be null or empty");
        }
        if (method == null || method.trim().isEmpty()) {
            throw new IllegalArgumentException("Method cannot be null or empty");
        }
        if (uri == null || uri.trim().isEmpty()) {
            throw new IllegalArgumentException("URI cannot be null or empty");
        }
        this.protocol = protocol;
        this.method = method;
        this.uri = uri;
        this.headers = headers != null ? new LinkedHashMap<>(headers) : new LinkedHashMap<>();
        this.body = body != null ? body.clone() : null;
    }

    /**
     * Constructs a new {@code Request} instance by reading and parsing data from the given {@code Socket}.
     *
     * <p>This constructor performs the following steps:
     * <ol>
     *     <li>Validates the socket's connection status.</li>
     *     <li>Reads the request line (e.g., "GET /index.html HTTP/1.1").</li>
     *     <li>Parses the request line into method, URI, and protocol.</li>
     *     <li>Handles custom protocols like "HIVE" by skipping header and body processing.</li>
     *     <li>Reads and parses the request headers.</li>
     *     <li>Reads the request body based on the "Content-Length" header, if present.</li>
     * </ol>
     * </p>
     *
     * <p>For requests using the "HIVE" protocol, headers and body are not processed, and the corresponding fields are set accordingly.</p>
     *
     * @param socket the {@link Socket} connected to the client
     * @throws IOException if an I/O error occurs while reading from the socket
     * @throws SocketException if the socket is closed, not connected, or the request format is invalid
     */
    public Request(Socket socket) throws IOException {
        // Validate socket connection
        if (socket.isClosed() || !socket.isConnected()) {
            throw new SocketException("Socket closed or not connected");
        }

        InputStream clientInputStream = socket.getInputStream();

        // Read the client's request line
        String requestLine = StreamUtil.readLine(clientInputStream);

        if (requestLine == null || requestLine.isEmpty()) {
            throw new SocketException("Received empty request from client.");
        }

        // Parse the request line
        String[] requestParts = requestLine.split(" ");
        if (requestParts.length != 3) {
            throw new SocketException("Bad Request: Invalid request line format");
        }

        this.method = requestParts[0];
        this.uri = requestParts[1];
        this.protocol = requestParts[2];

        // Handle custom protocol "HIVE"
        if (protocol.equalsIgnoreCase("HIVE")) {
            this.headers = new LinkedHashMap<>();
            this.body = null;
            return;
        }

        // Read the request headers
        this.headers = StreamUtil.readHeaders(clientInputStream);

        // Read the request body if "Content-Length" header is present
        if (headers.containsKey("content-length")) {
            int contentLength = Integer.parseInt(headers.get("content-length"));
            if (contentLength < 0) {
                throw new SocketException("Bad Request: Negative Content-Length");
            }
            this.body = StreamUtil.readRequestBody(clientInputStream, contentLength);
        } else {
            this.body = null;
        }
    }

    /**
     * Retrieves the protocol used for the request.
     *
     * @return the protocol as a {@code String} (e.g., "HTTP/1.1", "HIVE")
     */
    public String getProtocol() {
        return protocol;
    }

    /**
     * Retrieves the HTTP method of the request.
     *
     * @return the HTTP method as a {@code String} (e.g., "GET", "POST")
     */
    public String getMethod() {
        return method;
    }

    /**
     * Retrieves the URI requested by the client.
     *
     * @return the URI as a {@code String} (e.g., "/index.html")
     */
    public String getUri() {
        return uri;
    }

    /**
     * Retrieves the headers of the request.
     *
     * @return an unmodifiable {@link Map} containing header names and their corresponding values
     */
    public Map<String, String> getHeaders() {
        return Map.copyOf(headers);
    }

    /**
     * Retrieves the body of the request.
     *
     * @return the request body as a byte array, or {@code null} if no body is present
     */
    public byte[] getBody() {
        return body != null ? body.clone() : null;
    }

    /**
     * Logs the details of the request using the {@link Logger}.
     *
     * <p>The log entry includes the HTTP method, URI, protocol, headers, and body (if present).</p>
     *
     * <p>Example log output:
     * <pre>
     * GET /index.html HTTP/1.1
     * {Host=example.com, Content-Length=123}
     * {"key":"value"}
     * </pre>
     * </p>
     */
    public void log() {
        StringBuilder logBuilder = new StringBuilder();
        logBuilder.append(method)
                .append(" ")
                .append(uri)
                .append(" ")
                .append(protocol)
                .append("\n")
                .append(headers)
                .append("\n");

        if (body != null) {
            logBuilder.append(new String(body, StandardCharsets.UTF_8));
        } else {
            logBuilder.append("No Body");
        }

        Logger.info(logBuilder.toString());
    }
}
