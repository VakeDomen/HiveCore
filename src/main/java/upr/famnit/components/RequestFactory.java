package upr.famnit.components;

/**
 * The {@code RequestFactory} class serves as a utility for creating predefined {@link Request} instances.
 *
 * <p>This factory class provides static methods to generate specific types of requests that are commonly used
 * within the application. By centralizing the creation of these requests, the class promotes consistency and
 * reduces duplication across the codebase.</p>
 *
 * <p>Currently, the factory supports the creation of:
 * <ul>
 *     <li>An empty queue response represented by a "PONG" message in the "HIVE" protocol.</li>
 *     <li>An authentication response containing the authenticated user's name.</li>
 * </ul>
 * </p>
 *
 * <p>Additional factory methods can be added as needed to support more request types, enhancing scalability
 * and maintainability.</p>
 *
 * @see Request
 */
public class RequestFactory {

    /**
     * Creates a {@link Request} instance representing an empty queue response.
     *
     * <p>This method generates a request with the following characteristics:
     * <ul>
     *     <li><strong>Protocol:</strong> "HIVE"</li>
     *     <li><strong>Method:</strong> "PONG"</li>
     *     <li><strong>URI:</strong> "/"</li>
     *     <li><strong>Headers:</strong> {@code null} (no headers)</li>
     *     <li><strong>Body:</strong> {@code null} (no body)</li>
     * </ul>
     * </p>
     *
     * <p>This type of request is typically used to acknowledge receipt of a message or to indicate that
     * the queue is currently empty.</p>
     *
     * @return a new {@link Request} instance representing an empty queue response
     */
    public static Request EmptyQueResponse() {
        return new Request(
                "HIVE",
                "PONG",
                "/",
                null,
                null
        );
    }

    /**
     * Creates a {@link Request} instance representing an authentication response.
     *
     * <p>This method generates a request with the following characteristics:
     * <ul>
     *     <li><strong>Protocol:</strong> "HIVE"</li>
     *     <li><strong>Method:</strong> "AUTH"</li>
     *     <li><strong>URI:</strong> the provided {@code name}</li>
     *     <li><strong>Headers:</strong> {@code null} (no headers)</li>
     *     <li><strong>Body:</strong> {@code null} (no body)</li>
     * </ul>
     * </p>
     *
     * <p>This type of request is typically used to respond to authentication attempts, providing
     * the authenticated user's name as part of the response.</p>
     *
     * @param name the name of the authenticated user to include in the authentication response
     * @return a new {@link Request} instance representing an authentication response
     * @throws IllegalArgumentException if {@code name} is {@code null} or empty
     */
    public static Request AuthenticationResponse(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Authentication response name cannot be null or empty");
        }
        return new Request(
                "HIVE",
                "AUTH",
                name,
                null,
                null
        );
    }
}
