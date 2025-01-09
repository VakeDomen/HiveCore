package upr.famnit.authentication;

/**
 * The {@code SubmittedKey} class represents the data structure for key submissions
 * received from clients. It encapsulates the essential information required to
 * create a new authentication key within the system.
 *
 * <p>This class is typically used to deserialize JSON payloads from client requests
 * when clients submit new keys for insertion. It contains only the necessary fields
 * that clients are allowed to provide, ensuring that sensitive fields like {@code id}
 * and {@code value} are generated server-side.</p>
 *
 * <p>By separating the client-submitted data into its own class, the system maintains
 * a clear boundary between externally provided data and internally managed data,
 * enhancing security and integrity.</p>
 *
 * @see Key
 */
public class SubmittedKey {

    /**
     * The name associated with the submitted key.
     *
     * <p>This typically represents the owner or the purpose of the key.</p>
     */
    private final String name;

    /**
     * The role assigned to the submitted key as a {@code String}.
     *
     * <p>Clients specify the role to determine the access level and permissions
     * that the key will grant.</p>
     */
    private final String role;

    /**
     * Constructs a new {@code SubmittedKey} instance with the specified name and role.
     *
     * <p>This constructor is used when creating a {@code SubmittedKey} from client-provided
     * data, such as deserializing JSON from an HTTP request.</p>
     *
     * @param name the name associated with the key
     * @param role the role assigned to the key as a {@code String}
     * @throws IllegalArgumentException if {@code name} or {@code role} is {@code null} or empty
     */
    public SubmittedKey(String name, String role) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Name cannot be null or empty");
        }
        if (role == null || role.trim().isEmpty()) {
            throw new IllegalArgumentException("Role cannot be null or empty");
        }
        this.name = name;
        this.role = role;
    }

    /**
     * Retrieves the name associated with the submitted key.
     *
     * @return the {@code name} of the key
     */
    public String getName() {
        return name;
    }

    /**
     * Retrieves the role assigned to the submitted key.
     *
     * @return the {@code role} of the key as a {@code String}
     */
    public String getRole() {
        return role;
    }
}
