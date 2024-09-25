package upr.famnit.authentication;

public enum Role {
    Client,
    Worker,
    Admin,
    Unknown;

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
