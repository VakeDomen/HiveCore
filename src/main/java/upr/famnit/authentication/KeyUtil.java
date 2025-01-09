package upr.famnit.authentication;

import upr.famnit.components.LogLevel;
import upr.famnit.managers.DatabaseManager;
import upr.famnit.util.Logger;

import javax.management.openmbean.InvalidKeyException;
import java.sql.SQLException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * The {@code KeyUtil} class provides utility methods for handling and verifying
 * authentication keys. It includes functionality to fetch key details from the
 * database and verify key roles based on different verification types.
 *
 * <p>This class incorporates a caching mechanism to store fetched keys, minimizing
 * database access and enhancing performance.</p>
 *
 * <p>Note: This cache does not implement eviction policies. Depending on the application's
 * requirements, consider integrating an eviction strategy or using a caching library
 * like Caffeine or Guava for more advanced caching features.</p>
 */
public class KeyUtil {

    /**
     * A thread-safe cache that stores {@link Key} objects keyed by their string values.
     *
     * <p>This cache helps reduce the number of database queries by storing recently
     * accessed keys. Once a key is fetched from the database, it is stored in this
     * cache for quick future access.</p>
     */
    private static final ConcurrentMap<String, Key> cache = new ConcurrentHashMap<>();

    /**
     * Retrieves the name associated with the given key.
     *
     * <p>This method first attempts to retrieve the key from the cache. If the key
     * is not present in the cache, it fetches the key from the database and caches it
     * for future use.</p>
     *
     * @param key the string representation of the key
     * @return the name associated with the key
     * @throws InvalidKeyException if the key is invalid or cannot be fetched from the database
     */
    public static String nameKey(String key) {
        Key fetchedKey = getKeyFromCacheOrDB(key);
        if (fetchedKey == null) {
            throw new InvalidKeyException("Can't find key in database: " + key);
        }
        return fetchedKey.getName();
    }

    /**
     * Retrieves the role associated with the given key.
     *
     * <p>This method first attempts to retrieve the key from the cache. If the key
     * is not present in the cache, it fetches the key from the database and caches it
     * for future use.</p>
     *
     * @param key the string representation of the key
     * @return the {@link Role} associated with the key, or {@code Role.Unknown} if the key is invalid
     */
    public static Role getKeyRole(String key) {
        Key fetchedKey = getKeyFromCacheOrDB(key);
        if (fetchedKey == null) {
            Logger.error("Can't find key in database: " + key);
            return Role.Unknown;
        }
        return fetchedKey.getRole();
    }

    /**
     * Verifies the validity and role of the given key based on the specified verification type.
     *
     * <p>This method first attempts to retrieve the key from the cache. If the key
     * is not present in the cache, it fetches the key from the database and caches it
     * for future use.</p>
     *
     * <p>The verification logic is as follows:
     * <ul>
     *     <li>{@code NodeConnection}: The key must have a role of {@code Admin} or {@code Worker}.</li>
     *     <li>{@code ClientRequest}: The key must have a role of {@code Admin} or {@code Client}.</li>
     *     <li>{@code KeyCreation}: The key must have a role of {@code Admin}.</li>
     *     <li>Any other verification type results in verification failure.</li>
     * </ul>
     * </p>
     *
     * @param key the string representation of the key
     * @param type the {@link VerificationType} specifying the context of verification
     * @return {@code true} if the key is valid and meets the role requirements; {@code false} otherwise
     */
    public static boolean verifyKey(String key, VerificationType type) {
        Key fetchedKey = getKeyFromCacheOrDB(key);
        if (fetchedKey == null) {
            Logger.error("Can't find key in database: " + key);
            return false;
        }

        return switch (type) {
            case NodeConnection -> fetchedKey.getRole() == Role.Admin || fetchedKey.getRole() == Role.Worker;
            case ClientRequest -> fetchedKey.getRole() == Role.Admin || fetchedKey.getRole() == Role.Client;
            case KeyCreation -> fetchedKey.getRole() == Role.Admin;
            default -> false;
        };
    }

    /**
     * Retrieves a {@link Key} object from the cache or fetches it from the database if not present in the cache.
     *
     * <p>If the key is fetched from the database, it is stored in the cache for future access.</p>
     *
     * @param key the string representation of the key
     * @return the {@link Key} object if found; otherwise, {@code null}
     */
    private static Key getKeyFromCacheOrDB(String key) {
        // Attempt to retrieve the key from the cache
        Key fetchedKey = cache.get(key);
        if (fetchedKey != null) {
            return fetchedKey;
        }

        // If not in cache, fetch from the database
        try {
            fetchedKey = DatabaseManager.getKeyByValue(key);
            if (fetchedKey != null) {
                cache.put(key, fetchedKey);
            }
            return fetchedKey;
        } catch (SQLException e) {
            Logger.error("Failed fetching key from database: " + key + ". Error: " + e.getMessage());
            return null;
        }
    }
}
