package upr.famnit.authentication;

import upr.famnit.components.LogLevel;
import upr.famnit.managers.DatabaseManager;
import upr.famnit.util.Logger;

import javax.management.openmbean.InvalidKeyException;
import java.sql.SQLException;

public class KeyUtil {
    public static String nameKey(String key) {
        Key fethcedKey;
        try {
            fethcedKey = DatabaseManager.getKeyByValue(key);
        } catch (SQLException e) {
            throw new InvalidKeyException("Failed fetching key from database: " + key);
        }

        if (fethcedKey == null) {
            throw new InvalidKeyException("Can't find key in database: " + key);
        }

        return fethcedKey.getName();
    }

    public static Role getKeyRole(String key) {
        Key fethcedKey;
        try {
            fethcedKey = DatabaseManager.getKeyByValue(key);
        } catch (SQLException e) {
            Logger.error("Failed fetching key from database: " + key);
            return Role.Unknown;
        }

        if (fethcedKey == null) {
            Logger.error("Can't find key in database: " + key);
            return Role.Unknown;
        }

        return fethcedKey.getRole();
    }

    public static boolean verifyKey(String key, VerificationType type) {
        Key fethcedKey;
        try {
            fethcedKey = DatabaseManager.getKeyByValue(key);
        } catch (SQLException e) {
            Logger.error("Failed fetching key from database: " + key);
            return false;
        }

        if (fethcedKey == null) {
            Logger.error("Can't find key in database: " + key);
            return false;
        }

        switch (type) {
            case NodeConnection -> {
                return fethcedKey.getRole() == Role.Admin || fethcedKey.getRole() == Role.Worker;
            }
            case ClientRequest -> {
                return fethcedKey.getRole() == Role.Admin || fethcedKey.getRole() == Role.Client;
            }
            case KeyCreation -> {
                return fethcedKey.getRole() == Role.Admin;
            }
            default -> {
                return false;
            }
        }
    }
}
