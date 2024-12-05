package upr.famnit.authentication;

import java.util.NoSuchElementException;
import java.util.UUID;

public class Key {
    private final int id;
    private final String name;
    private final String value;
    private final Role role;

    public Key(SubmittedKey submittedKey) {
        this.id = -1;
        this.name = submittedKey.getName();
        this.value = String.valueOf(UUID.randomUUID());
        this.role = RoleUtil.fromString(submittedKey.getRole());
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
