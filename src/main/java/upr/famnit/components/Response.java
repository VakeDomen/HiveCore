package upr.famnit.components;

import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * The {@code Response} class represents an HTTP-like response that the server sends back to the client.
 *
 * <p>This class encapsulates the details of a server's response, including the protocol version,
 * status code, status text, headers, and an optional body. It provides constructors for creating
 * responses with or without a body and exposes getter methods to access each component of the response.</p>
 *
 * <p>Instances of {@code Response} are immutable once created, ensuring thread safety in multi-threaded
 * environments.</p>
 *
 * <p>The class is designed to handle standard HTTP responses and can be extended to support custom protocols as needed.</p>
 *
 * @see upr.famnit.components.Request
 */
public class Response {

    /**
     * The protocol version used for the response (e.g., "HTTP/1.1", "HIVE").
     */
    private final String protocol;

    /**
     * The HTTP status code of the response (e.g., 200, 404).
     */
    private final int code;

    /**
     * The textual reason phrase corresponding to the status code (e.g., "OK", "Not Found").
     */
    private final String text;

    /**
     * A map containing the response headers. Each key-value pair represents a header name and its corresponding value.
     */
    private final Map<String, String> headers;

    /**
     * The body of the response as a byte array. It is {@code null} if the response does not contain a body.
     */
    private final byte[] body;

    /**
     * Constructs a new {@code Response} instance without a body.
     *
     * <p>This constructor is typically used for responses that do not require a body, such as
     * simple acknowledgments or status updates.</p>
     *
     * @param protocol the protocol version used for the response (e.g., "HTTP/1.1", "HIVE")
     * @param code the HTTP status code of the response (e.g., 200, 404)
     * @param text the textual reason phrase corresponding to the status code (e.g., "OK", "Not Found")
     * @param headers a map of header names and their corresponding values
     * @throws IllegalArgumentException if {@code protocol}, {@code text}, or {@code headers} is {@code null} or invalid
     */
    public Response(String protocol, int code, String text, Map<String, String> headers) {
        if (protocol == null || protocol.trim().isEmpty()) {
            throw new IllegalArgumentException("Protocol cannot be null or empty");
        }
        if (text == null || text.trim().isEmpty()) {
            throw new IllegalArgumentException("Status text cannot be null or empty");
        }
        if (headers == null) {
            throw new IllegalArgumentException("Headers map cannot be null");
        }
        this.protocol = protocol;
        this.code = code;
        this.text = text;
        this.headers = Map.copyOf(headers);
        this.body = null;
    }

    /**
     * Constructs a new {@code Response} instance with a body.
     *
     * <p>This constructor is typically used for responses that include a body, such as HTML pages,
     * JSON payloads, or binary data.</p>
     *
     * @param protocol the protocol version used for the response (e.g., "HTTP/1.1", "HIVE")
     * @param code the HTTP status code of the response (e.g., 200, 404)
     * @param text the textual reason phrase corresponding to the status code (e.g., "OK", "Not Found")
     * @param headers a map of header names and their corresponding values
     * @param body the body of the response as a byte array
     * @throws IllegalArgumentException if {@code protocol}, {@code text}, or {@code headers} is {@code null} or invalid
     */
    public Response(String protocol, int code, String text, Map<String, String> headers, byte[] body) {
        if (protocol == null || protocol.trim().isEmpty()) {
            throw new IllegalArgumentException("Protocol cannot be null or empty");
        }
        if (text == null || text.trim().isEmpty()) {
            throw new IllegalArgumentException("Status text cannot be null or empty");
        }
        if (headers == null) {
            throw new IllegalArgumentException("Headers map cannot be null");
        }
        this.protocol = protocol;
        this.code = code;
        this.text = text;
        this.headers = Map.copyOf(headers);
        this.body = body != null ? body.clone() : null;
    }

    /**
     * Retrieves the protocol version used for the response.
     *
     * @return the protocol as a {@code String} (e.g., "HTTP/1.1", "HIVE")
     */
    public String getProtocol() {
        return protocol;
    }

    /**
     * Retrieves the HTTP status code of the response.
     *
     * @return the HTTP status code as an {@code int} (e.g., 200, 404)
     */
    public int getCode() {
        return code;
    }

    /**
     * Retrieves the textual reason phrase corresponding to the status code.
     *
     * @return the status text as a {@code String} (e.g., "OK", "Not Found")
     */
    public String getText() {
        return text;
    }

    /**
     * Retrieves the headers of the response.
     *
     * @return an unmodifiable {@link Map} containing header names and their corresponding values
     */
    public Map<String, String> getHeaders() {
        return Map.copyOf(headers);
    }

    /**
     * Retrieves the body of the response.
     *
     * @return the response body as a byte array, or {@code null} if no body is present
     */
    public byte[] getBody() {
        return body != null ? body.clone() : null;
    }

    /**
     * Provides a string representation of the {@code Response} instance.
     *
     * <p>The returned string includes the protocol, status code, status text, headers,
     * and the body (if present) as a UTF-8 encoded string.</p>
     *
     * @return a string representation of the response
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(protocol)
                .append(" ")
                .append(code)
                .append(" ")
                .append(text)
                .append("\n")
                .append(headers)
                .append("\n");
        if (body != null) {
            sb.append(new String(body, StandardCharsets.UTF_8));
        } else {
            sb.append("No Body");
        }
        return sb.toString();
    }

    /**
     * Determines whether two {@code Response} instances are equal based on their protocol, status code,
     * status text, headers, and body.
     *
     * @param obj the object to compare with this response
     * @return {@code true} if the specified object is equal to this response; {@code false} otherwise
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Response)) return false;
        Response other = (Response) obj;
        return this.code == other.code &&
                this.protocol.equals(other.protocol) &&
                this.text.equals(other.text) &&
                this.headers.equals(other.headers) &&
                java.util.Arrays.equals(this.body, other.body);
    }

}
