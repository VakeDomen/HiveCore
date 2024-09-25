package upr.famnit.authentication;

public class Key {
    private final int id;
    private final String name;
    private final String value;
    private final Role role;

    public Key(int id, String name, String value, Role role) {
        this.id = id;
        this.name = name;
        this.value = value;
        this.role = role;
    }

    public Key(String name, String value, Role role) {
        this.id = -1;
        this.name = name;
        this.value = value;
        this.role = role;
    }

    public Key(int id, String name, String value, String role) {
        this.id = id;
        this.name = name;
        this.value = value;
        this.role = RoleUtil.fromString(role);
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }

    public Role getRole() {
        return role;
    }
}
