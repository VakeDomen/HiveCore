package upr.famnit.authentication;

/**
 * The {@code Role} enum represents the various roles that an authentication key
 * can possess within the system. Each role defines a specific level of access
 * and permissions granted to the entity associated with the key.
 *
 * <p>The available roles are:
 * <ul>
 *     <li>{@code Client}: Represents a standard client with limited access rights.</li>
 *     <li>{@code Worker}: Represents a worker entity with elevated permissions.</li>
 *     <li>{@code Admin}: Represents an administrator with full access rights.</li>
 *     <li>{@code Unknown}: Represents an undefined or unrecognized role.</li>
 * </ul>
 * </p>
 *
 * <p>This enum provides a custom {@code toString()} method to return the role's name
 * as a {@code String}.</p>
 *
 * @see RoleUtil
 */
public enum Role {
    /**
     * Represents a standard client with limited access rights.
     */
    Client,

    /**
     * Represents a worker entity with elevated permissions.
     */
    Worker,

    /**
     * Represents an administrator with full access rights.
     */
    Admin,

    /**
     * Represents an undefined or unrecognized role.
     */
    Unknown;

    /**
     * Returns the name of the role as a {@code String}.
     *
     * <p>This method overrides the default {@code toString()} implementation
     * to provide a consistent string representation of the role.</p>
     *
     * @return the name of the role
     */
    @Override
    public String toString() {
        switch (this) {
            case Client -> {
                return "Client";
            }
            case Worker -> {
                return "Worker";
            }
            case Admin -> {
                return "Admin";
            }
            default -> {
                return "Unknown";
            }
        }
    }
}
