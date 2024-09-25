package upr.famnit.authentication;

public class RoleUtil {
    public static Role fromString(String role) {
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

    public static String toString(Role role) {
        switch (role) {
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
