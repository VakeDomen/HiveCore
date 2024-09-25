package upr.famnit.managers;

import upr.famnit.authentication.Key;
import upr.famnit.util.Logger;

import java.sql.*;
import java.util.ArrayList;

import static upr.famnit.util.Config.DATABASE_URL;

public class DatabaseManager {

    private static Connection connection;

    public static Connection connect() throws SQLException {
        if (connection == null || connection.isClosed()) {
            connection = DriverManager.getConnection(DATABASE_URL);
        }
        return connection;
    }

    public static synchronized void createKeysTable() throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS keys (\n"
                + "     id INTEGER PRIMARY KEY AUTOINCREMENT,\n"
                + "     name TEXT NOT NULL UNIQUE,\n"
                + "     value TEXT NOT NULL,\n"
                + "     role TEXT NOT NULL\n"
                + ");";

        try (Connection conn = connect(); Statement statement = conn.createStatement()) {
            statement.execute(sql);
            Logger.log("Keys table created or already exists.");
        }

//        insertKey(new Key("Admin", "4fd4bd9f-e748-42f9-b486-771fa78cff7c", Role.Admin));
    }

    public static void insertKey(Key key) throws SQLException {
        String sql = "INSERT INTO keys(name, value, role) VALUES(?, ?, ?)";
        try (Connection conn = connect(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, key.getName());
            stmt.setString(2, key.getValue());
            stmt.setString(3, key.getRole().toString());
            stmt.executeUpdate();
            Logger.log("Key inserted successfully.");
        }
    }

    public static Key getKeyByValue(String value) throws SQLException {
        String sql = "SELECT * FROM keys WHERE value = ?";
        try (Connection conn = connect(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, value);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return new Key(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("value"),
                        rs.getString("role")
                );
            } else {
                return null;
            }
        }
    }

    public static ArrayList<Key> getAllKeys() throws SQLException {
        String sql = "SELECT * FROM keys";
        ArrayList<Key> keys = new ArrayList<>();
        try (Connection conn = connect(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                keys.add(new Key(
                    rs.getInt("id"),
                    rs.getString("name"),
                    rs.getString("value"),
                    rs.getString("role")
                ));
            }
        }
        return keys;
    }
}
