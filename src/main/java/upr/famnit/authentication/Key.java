package upr.famnit.authentication;

import java.util.NoSuchElementException;
import java.util.UUID;

/**
 * The {@code Key} class represents an authentication key within the system.
 *
 * <p>Each key is uniquely identified by its {@code value} and is associated with
 * a specific {@link Role}. Keys are used to authenticate and authorize users or
 * systems performing various operations within the application.</p>
 *
 * <p>The class provides constructors for creating new keys based on submitted
 * key information or existing key details from persistent storage.</p>
 */
public class Key {

    /**
     * The unique identifier for the key.
     *
     * <p>This identifier is typically assigned by the database upon key creation.
     * A value of {@code -1} indicates that the key has not been persisted yet.</p>
     */
    private final int id;

    /**
     * The name associated with the key.
     *
     * <p>This name typically represents the owner or the purpose of the key.</p>
     */
    private final String name;

    /**
     * The unique string value of the key.
     *
     * <p>This value is used for authentication purposes and is generated as a UUID
     * when creating a new key from submitted key information.</p>
     */
    private final String value;

    /**
     * The role assigned to the key.
     *
     * <p>The role determines the level of access and permissions granted to the
     * entity possessing this key.</p>
     *
     * @see Role
     */
    private final Role role;

    /**
     * Constructs a new {@code Key} instance based on the provided {@code SubmittedKey}.
     *
     * <p>This constructor is typically used when a new key is submitted for creation.
     * It generates a unique {@code value} for the key and assigns a default {@code id}
     * of {@code -1}, indicating that the key has not yet been persisted to the database.</p>
     *
     * @param submittedKey the {@link SubmittedKey} containing information for the new key
     * @throws NullPointerException if {@code submittedKey} is {@code null}
     */
    public Key(SubmittedKey submittedKey) {
        if (submittedKey == null) {
            throw new NullPointerException("SubmittedKey cannot be null");
        }
        this.id = -1;
        this.name = submittedKey.getName();
        this.value = UUID.randomUUID().toString();
        this.role = RoleUtil.fromString(submittedKey.getRole());
    }

    /**
     * Constructs a new {@code Key} instance with specified details.
     *
     * <p>This constructor is typically used when retrieving key information from
     * persistent storage, such as a database.</p>
     *
     * @param id the unique identifier of the key
     * @param name the name associated with the key
     * @param value the unique string value of the key
     * @param role the role assigned to the key as a {@code String}
     * @throws IllegalArgumentException if {@code role} does not correspond to a valid {@link Role}
     */
    public Key(int id, String name, String value, String role) {
        if (name == null || value == null || role == null) {
            throw new IllegalArgumentException("Name, value, and role cannot be null");
        }
        this.id = id;
        this.name = name;
        this.value = value;
        this.role = RoleUtil.fromString(role);
    }

    /**
     * Retrieves the unique identifier of the key.
     *
     * @return the {@code id} of the key
     */
    public int getId() {
        return id;
    }

    /**
     * Retrieves the name associated with the key.
     *
     * @return the {@code name} of the key
     */
    public String getName() {
        return name;
    }

    /**
     * Retrieves the unique string value of the key.
     *
     * @return the {@code value} of the key
     */
    public String getValue() {
        return value;
    }

    /**
     * Retrieves the role assigned to the key.
     *
     * @return the {@link Role} of the key
     */
    public Role getRole() {
        return role;
    }

    /**
     * Provides a string representation of the {@code Key} instance.
     *
     * <p>The returned string includes the key's {@code id}, {@code name}, {@code value},
     * and {@code role}.</p>
     *
     * @return a string representation of the key
     */
    @Override
    public String toString() {
        return "Key{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", value='" + value + '\'' +
                ", role=" + role +
                '}';
    }

    /**
     * Determines whether two {@code Key} instances are equal based on their {@code value}.
     *
     * <p>Two keys are considered equal if their {@code value} fields are identical.</p>
     *
     * @param obj the object to compare with this key
     * @return {@code true} if the specified object is equal to this key; {@code false} otherwise
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Key)) return false;
        Key other = (Key) obj;
        return this.value.equals(other.value);
    }

    /**
     * Returns the hash code value for the key based on its {@code value}.
     *
     * @return the hash code of the key
     */
    @Override
    public int hashCode() {
        return value.hashCode();
    }
}
