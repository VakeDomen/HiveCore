package upr.famnit.managers;

import upr.famnit.authentication.Key;
import upr.famnit.util.Logger;

import java.sql.*;
import java.util.ArrayList;

import static upr.famnit.util.Config.DATABASE_URL;

/**
 * The {@code DatabaseManager} class provides utility methods for interacting with the application's SQLite database.
 *
 * <p>This class manages the database connection and offers methods to create necessary tables,
 * insert new keys, retrieve keys by value, and fetch all keys from the database. It ensures that
 * database operations are performed safely and efficiently, handling SQL exceptions and maintaining
 * the integrity of the data.</p>
 *
 * <p>All methods in this class are synchronized to ensure thread safety when accessed by multiple threads
 * concurrently. This is crucial in a multithreaded environment where multiple clients might be interacting
 * with the database simultaneously.</p>
 *
 * <p>The class follows the Singleton pattern for the database connection, ensuring that only one connection
 * instance exists throughout the application's lifecycle. This approach conserves resources and maintains
 * consistent access to the database.</p>
 *
 * @see Key
 * @see Logger
 */
public class DatabaseManager {

    /**
     * The singleton {@link Connection} instance for interacting with the SQLite database.
     */
    private static Connection connection;

    /**
     * Establishes a connection to the SQLite database using the configured database URL.
     *
     * <p>If a connection already exists and is open, it returns the existing connection.
     * Otherwise, it creates a new connection and returns it.</p>
     *
     * @return the {@link Connection} object for interacting with the database
     * @throws SQLException if a database access error occurs or the URL is invalid
     */
    public static Connection connect() throws SQLException {
        if (connection == null || connection.isClosed()) {
            connection = DriverManager.getConnection(DATABASE_URL);
            Logger.info("Database connection established.");
        }
        return connection;
    }

    /**
     * Creates the {@code keys} table in the database if it does not already exist.
     *
     * <p>The {@code keys} table stores information about authentication keys, including their
     * unique identifier, name, value, and associated role. This method ensures that the
     * table structure is in place before any key-related operations are performed.</p>
     *
     * @throws SQLException if a database access error occurs or the SQL statement is invalid
     */
    public static synchronized void createKeysTable() throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS keys (\n"
                + "     id INTEGER PRIMARY KEY AUTOINCREMENT,\n"
                + "     name TEXT NOT NULL UNIQUE,\n"
                + "     value TEXT NOT NULL,\n"
                + "     role TEXT NOT NULL\n"
                + ");";

        try (Connection conn = connect(); Statement statement = conn.createStatement()) {
            statement.execute(sql);
            Logger.info("Keys table created or already exists.");
        }
    }

    /**
     * Inserts a new {@link Key} into the {@code keys} table.
     *
     * <p>This method adds a new key with its name, value, and role to the database. It ensures that
     * the key name is unique to prevent duplicate entries.</p>
     *
     * @param key the {@link Key} object to be inserted into the database
     * @throws SQLException if a database access error occurs, the SQL statement is invalid, or the key name violates uniqueness
     */
    public static synchronized void insertKey(Key key) throws SQLException {
        String sql = "INSERT INTO keys(name, value, role) VALUES(?, ?, ?)";
        try (Connection conn = connect(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, key.getName());
            stmt.setString(2, key.getValue());
            stmt.setString(3, key.getRole().toString());
            stmt.executeUpdate();
            Logger.success("Key inserted successfully: " + key.getName());
        }
    }

    /**
     * Retrieves a {@link Key} from the {@code keys} table based on its value.
     *
     * <p>This method searches for a key with the specified value and returns the corresponding
     * {@link Key} object if found. If no matching key is found, it returns {@code null}.</p>
     *
     * @param value the value of the key to be retrieved
     * @return the {@link Key} object if found; {@code null} otherwise
     * @throws SQLException if a database access error occurs or the SQL statement is invalid
     */
    public static synchronized Key getKeyByValue(String value) throws SQLException {
        String sql = "SELECT * FROM keys WHERE value = ?";
        try (Connection conn = connect(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, value);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                Key key = new Key(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("value"),
                        rs.getString("role")
                );
                Logger.info("Key retrieved: " + key.getName());
                return key;
            } else {
                Logger.warn("No key found with value: " + value);
                return null;
            }
        }
    }

    /**
     * Retrieves all {@link Key} entries from the {@code keys} table.
     *
     * <p>This method fetches all keys stored in the database and returns them as an {@link ArrayList}.
     * It is useful for administrative tasks or for displaying all available keys.</p>
     *
     * @return an {@link ArrayList} containing all {@link Key} objects from the database
     * @throws SQLException if a database access error occurs or the SQL statement is invalid
     */
    public static synchronized ArrayList<Key> getAllKeys() throws SQLException {
        String sql = "SELECT * FROM keys";
        ArrayList<Key> keys = new ArrayList<>();
        try (Connection conn = connect(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Key key = new Key(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("value"),
                        rs.getString("role")
                );
                keys.add(key);
            }
            Logger.info("Total keys retrieved: " + keys.size());
        }
        return keys;
    }
}
