package upr.famnit.components;

import java.util.Map;

/**
 * The {@code ResponseFactory} class serves as a utility for creating predefined {@link Response} instances.
 *
 * <p>This factory class provides static methods to generate standard HTTP responses commonly used within the application.
 * By centralizing the creation of these responses, the class promotes consistency, reduces duplication, and simplifies
 * the process of generating responses across different components of the system.</p>
 *
 * <p>Currently, the factory supports the creation of:
 * <ul>
 *     <li>OK responses with and without a body.</li>
 *     <li>Method Not Allowed response.</li>
 *     <li>Not Found response.</li>
 *     <li>Unauthorized response.</li>
 *     <li>Bad Request response.</li>
 * </ul>
 * </p>
 *
 * <p>Additional factory methods can be added as needed to support more response types, enhancing scalability and maintainability.</p>
 *
 * @see Response
 */
public class ResponseFactory {

    /**
     * Creates a {@link Response} instance representing a "Method Not Allowed" error.
     *
     * <p>This response indicates that the HTTP method used in the request is not supported by the server for the targeted resource.</p>
     *
     * <p>The response has the following characteristics:
     * <ul>
     *     <li><strong>Protocol:</strong> "HTTP/1.1"</li>
     *     <li><strong>Status Code:</strong> 405</li>
     *     <li><strong>Status Text:</strong> "Method Not Allowed"</li>
     *     <li><strong>Headers:</strong> "Content-Length: 0", "Connection: close"</li>
     *     <li><strong>Body:</strong> None</li>
     * </ul>
     * </p>
     *
     * @return a new {@link Response} instance representing a "Method Not Allowed" error
     */
    public static Response MethodNotAllowed() {
        return new Response(
                "HTTP/1.1",
                405,
                "Method Not Allowed",
                Map.of(
                        "Content-Length", "0",
                        "Connection", "close"
                )
        );
    }

    /**
     * Creates a {@link Response} instance representing a successful "OK" response without a body.
     *
     * <p>This response indicates that the request was successfully processed by the server, but there is no additional content to return.</p>
     *
     * <p>The response has the following characteristics:
     * <ul>
     *     <li><strong>Protocol:</strong> "HTTP/1.1"</li>
     *     <li><strong>Status Code:</strong> 200</li>
     *     <li><strong>Status Text:</strong> "OK"</li>
     *     <li><strong>Headers:</strong> "Content-Length: 0", "Connection: close"</li>
     *     <li><strong>Body:</strong> None</li>
     * </ul>
     * </p>
     *
     * @return a new {@link Response} instance representing a successful "OK" response without a body
     */
    public static Response Ok() {
        return new Response(
                "HTTP/1.1",
                200,
                "OK",
                Map.of(
                        "Content-Length", "0",
                        "Connection", "close"
                )
        );
    }

    /**
     * Creates a {@link Response} instance representing a successful "OK" response with a body.
     *
     * <p>This response indicates that the request was successfully processed by the server and includes a body containing the response data.</p>
     *
     * <p>The response has the following characteristics:
     * <ul>
     *     <li><strong>Protocol:</strong> "HTTP/1.1"</li>
     *     <li><strong>Status Code:</strong> 200</li>
     *     <li><strong>Status Text:</strong> "OK"</li>
     *     <li><strong>Headers:</strong> "Content-Length: &lt;body length&gt;", "Connection: close"</li>
     *     <li><strong>Body:</strong> The provided byte array content</li>
     * </ul>
     * </p>
     *
     * @param body the body content of the response as a byte array
     * @return a new {@link Response} instance representing a successful "OK" response with a body
     * @throws IllegalArgumentException if {@code body} is {@code null}
     */
    public static Response Ok(byte[] body) {
        if (body == null) {
            throw new IllegalArgumentException("Response body cannot be null");
        }
        return new Response(
                "HTTP/1.1",
                200,
                "OK",
                Map.of(
                        "Content-Length", String.valueOf(body.length),
                        "Connection", "close"
                ),
                body
        );
    }

    /**
     * Creates a {@link Response} instance representing a "Not Found" error.
     *
     * <p>This response indicates that the requested resource could not be found on the server.</p>
     *
     * <p>The response has the following characteristics:
     * <ul>
     *     <li><strong>Protocol:</strong> "HTTP/1.1"</li>
     *     <li><strong>Status Code:</strong> 404</li>
     *     <li><strong>Status Text:</strong> "Not Found"</li>
     *     <li><strong>Headers:</strong> "Content-Length: 0", "Connection: close"</li>
     *     <li><strong>Body:</strong> None</li>
     * </ul>
     * </p>
     *
     * @return a new {@link Response} instance representing a "Not Found" error
     */
    public static Response NotFound() {
        return new Response(
                "HTTP/1.1",
                404,
                "Not Found",
                Map.of(
                        "Content-Length", "0",
                        "Connection", "close"
                )
        );
    }

    /**
     * Creates a {@link Response} instance representing an "Unauthorized" error.
     *
     * <p>This response indicates that the request requires user authentication or that the provided authentication credentials are invalid.</p>
     *
     * <p>The response has the following characteristics:
     * <ul>
     *     <li><strong>Protocol:</strong> "HTTP/1.1"</li>
     *     <li><strong>Status Code:</strong> 403</li>
     *     <li><strong>Status Text:</strong> "Unauthorized"</li>
     *     <li><strong>Headers:</strong> "Content-Length: 0", "Connection: close"</li>
     *     <li><strong>Body:</strong> None</li>
     * </ul>
     * </p>
     *
     * @return a new {@link Response} instance representing an "Unauthorized" error
     */
    public static Response Unauthorized() {
        return new Response(
                "HTTP/1.1",
                403,
                "Unauthorized",
                Map.of(
                        "Content-Length", "0",
                        "Connection", "close"
                )
        );
    }

    /**
     * Creates a {@link Response} instance representing a "Bad Request" error.
     *
     * <p>This response indicates that the server could not understand the request due to invalid syntax.</p>
     *
     * <p>The response has the following characteristics:
     * <ul>
     *     <li><strong>Protocol:</strong> "HTTP/1.1"</li>
     *     <li><strong>Status Code:</strong> 400</li>
     *     <li><strong>Status Text:</strong> "Bad Request"</li>
     *     <li><strong>Headers:</strong> "Content-Length: 0", "Connection: close"</li>
     *     <li><strong>Body:</strong> None</li>
     * </ul>
     * </p>
     *
     * @return a new {@link Response} instance representing a "Bad Request" error
     */
    public static Response BadRequest() {
        return new Response(
                "HTTP/1.1",
                400,
                "Bad Request",
                Map.of(
                        "Content-Length", "0",
                        "Connection", "close"
                )
        );
    }
}
