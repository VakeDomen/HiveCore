package upr.famnit.authentication;

public class SubmittedKey {
    private final String name;
    private final String role;

    public SubmittedKey(String name, String role) {
        this.name = name;
        this.role = role;
    }

    public String getName() {
        return name;
    }

    public String getRole() {
        return role;
    }
}
