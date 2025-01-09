package upr.famnit.authentication;

/**
 * The {@code RoleUtil} class provides utility methods for converting between
 * {@code String} representations of roles and their corresponding {@code Role} enum values.
 *
 * <p>This class facilitates the translation of role information received as strings,
 * such as from user input or external data sources, into the {@code Role} enum used
 * internally within the system.</p>
 *
 * <p>It ensures that role strings are interpreted in a case-insensitive manner and
 * provides a default role of {@code Role.Unknown} for unrecognized or invalid role strings.</p>
 *
 * @see Role
 */
public class RoleUtil {

    /**
     * Converts a {@code String} representation of a role to its corresponding {@code Role} enum value.
     *
     * <p>This method performs a case-insensitive comparison of the input string against known role names.
     * If the input string does not match any recognized roles, {@code Role.Unknown} is returned.</p>
     *
     * <p>Example usage:
     * <pre>{@code
     * Role role = RoleUtil.fromString("admin"); // Returns Role.Admin
     * Role unknownRole = RoleUtil.fromString("superuser"); // Returns Role.Unknown
     * }</pre>
     * </p>
     *
     * @param role the {@code String} representation of the role to be converted
     * @return the corresponding {@code Role} enum value, or {@code Role.Unknown} if the input does not match any known roles
     * @throws NullPointerException if the {@code role} parameter is {@code null}
     */
    public static Role fromString(String role) {
        if (role == null) {
            throw new NullPointerException("Role string cannot be null");
        }

        switch (role.toLowerCase()) {
            case "client" -> {
                return Role.Client;
            }
            case "worker" -> {
                return Role.Worker;
            }
            case "admin" -> {
                return Role.Admin;
            }
            default -> {
                return Role.Unknown;
            }
        }
    }
}
