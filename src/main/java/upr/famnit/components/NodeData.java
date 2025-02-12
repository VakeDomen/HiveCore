package upr.famnit.components;

import upr.famnit.authentication.VerificationStatus;
import upr.famnit.util.Logger;

import java.time.LocalDateTime;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * The {@code NodeData} class encapsulates the state and metadata of a node within the system.
 *
 * <p>This class maintains various attributes related to a node, such as its last ping timestamp,
 * connection exception count, node name, versions, verification status, nonce, and tags. It
 * employs a {@link ReadWriteLock} to ensure thread-safe access and modification of its fields,
 * allowing concurrent read operations while serializing write operations.</p>
 *
 * <p>Instances of {@code NodeData} are designed to be mutable, with synchronized methods managing
 * state changes. This ensures consistency and integrity of the node's data in multi-threaded
 * environments.</p>
 *
 * <p>Additionally, the class provides mechanisms to handle connection exceptions, verify node statuses,
 * and manage metadata such as version information and tags.</p>
 */
public class NodeData {

    /**
     * A {@link ReadWriteLock} to manage concurrent access to the node's status-related fields.
     *
     * <p>This lock allows multiple threads to read the status simultaneously while ensuring that
     * write operations are exclusive.</p>
     */
    private final ReadWriteLock statusLock = new ReentrantReadWriteLock();

    /**
     * The read lock derived from {@link #statusLock}.
     *
     * <p>Used to synchronize read-only access to the node's status-related fields.</p>
     */
    private final Lock statusReadLock = statusLock.readLock();

    /**
     * The write lock derived from {@link #statusLock}.
     *
     * <p>Used to synchronize write access to the node's status-related fields.</p>
     */
    private final Lock statusWriteLock = statusLock.writeLock();

    /**
     * The timestamp of the last successful ping from the node.
     */
    private LocalDateTime lastPing;

    /**
     * The name of the node.
     */
    private String nodeName;

    /**
     * The version of the node software.
     */
    private WorkerVersion nodeVersion;

    /**
     * The current verification status of the node.
     */
    private VerificationStatus verificationStatus;

    /**
     * The nonce associated with the node for verification purposes.
     */
    private String nonce;

    /**
     * The tags associated with the node.
     */
    private String tags;

    /**
     * Constructs a new {@code NodeData} instance with default initial values.
     *
     * <p>Upon creation, {@code lastPing} is set to the current time, {@code connectionExceptionCount}
     * is initialized to zero, and {@code verificationStatus} is set to {@link VerificationStatus#SettingUp}.
     * Other fields are initialized to {@code null}.</p>
     */
    public NodeData() {
        lastPing = LocalDateTime.now();
        nodeName = null;
        verificationStatus = VerificationStatus.SettingUp;
        nonce = null;
        tags = null;
    }

    /**
     * Updates the timestamp of the last successful ping from the node.
     *
     * <p>This method acquires the write lock to ensure exclusive access during the update.</p>
     *
     * @param lastPing the new {@link LocalDateTime} representing the last ping time
     */
    public void setLastPing(LocalDateTime lastPing) {
        statusWriteLock.lock();
        try {
            this.lastPing = lastPing;
        } finally {
            statusWriteLock.unlock();
        }
    }

    /**
     * Tests whether the provided tags differ from the current tags and updates them if necessary.
     *
     * <p>This method first acquires the read lock to compare the existing tags with the new tags.
     * If they differ, it acquires the write lock to update the tags, ensuring thread-safe modification.</p>
     *
     * @param tags the new tags to be set
     */
    public void tagsTestAndSet(String tags) {
        statusWriteLock.lock();
        try {
            this.tags = tags;
        } finally {
            statusWriteLock.unlock();
        }
    }


    /**
     * Sets the name of the node.
     *
     * <p>This method acquires the write lock to ensure exclusive access during the update.</p>
     *
     * @param nodeName the new name of the node
     */
    public void setNodeName(String nodeName) {
        statusWriteLock.lock();
        try {
            this.nodeName = nodeName;
        } finally {
            statusWriteLock.unlock();
        }
    }

    /**
     * Sets the verification status of the node.
     *
     * <p>This method acquires the write lock to ensure exclusive access during the update.</p>
     *
     * @param verificationStatus the new {@link VerificationStatus} of the node
     */
    public void setVerificationStatus(VerificationStatus verificationStatus) {
        statusWriteLock.lock();
        try {
            this.verificationStatus = verificationStatus;
        } finally {
            statusWriteLock.unlock();
        }
    }

    /**
     * Sets the nonce associated with the node for verification purposes.
     *
     * <p>This method acquires the write lock to ensure exclusive access during the update.</p>
     *
     * @param nonce the new nonce value
     */
    public void setNonce(String nonce) {
        statusWriteLock.lock();
        try {
            this.nonce = nonce;
        } finally {
            statusWriteLock.unlock();
        }
    }

    /**
     * Retrieves the timestamp of the last successful ping from the node.
     *
     * <p>This method acquires the read lock to ensure thread-safe access to the field.</p>
     *
     * @return the {@link LocalDateTime} representing the last ping time
     */
    public LocalDateTime getLastPing() {
        statusReadLock.lock();
        try {
            return lastPing;
        } finally {
            statusReadLock.unlock();
        }
    }

    /**
     * Retrieves the name of the node.
     *
     * <p>This method acquires the read lock to ensure thread-safe access to the field.</p>
     *
     * @return the node's name, or {@code null} if not set
     */
    public String getNodeName() {
        statusReadLock.lock();
        try {
            return nodeName;
        } finally {
            statusReadLock.unlock();
        }
    }

    /**
     * Retrieves the current verification status of the node.
     *
     * <p>This method acquires the read lock to ensure thread-safe access to the field.</p>
     *
     * @return the current {@link VerificationStatus} of the node
     */
    public VerificationStatus getVerificationStatus() {
        statusReadLock.lock();
        try {
            return verificationStatus;
        } finally {
            statusReadLock.unlock();
        }
    }

    /**
     * Retrieves the nonce associated with the node for verification purposes.
     *
     * <p>This method acquires the read lock to ensure thread-safe access to the field.</p>
     *
     * @return the nonce value, or {@code null} if not set
     */
    public String getNonce() {
        statusReadLock.lock();
        try {
            return nonce;
        } finally {
            statusReadLock.unlock();
        }
    }

    /**
     * Retrieves the tags associated with the node.
     *
     * <p>This method acquires the read lock to ensure thread-safe access to the field.</p>
     *
     * @return the tags as a {@code String}, or {@code null} if not set
     */
    public String getTags() {
        statusReadLock.lock();
        try {
            return tags;
        } finally {
            statusReadLock.unlock();
        }
    }

    /**
     * Sets the node software version.
     *
     * <p>This method acquires the write lock to ensure exclusive access during the update.</p>
     *
     * @param nodeVersion the new node version
     */
    public void setNodeVersion(WorkerVersion nodeVersion) {
        statusWriteLock.lock();
        try {
            this.nodeVersion = nodeVersion;
        } finally {
            statusWriteLock.unlock();
        }
    }

    /**
     * Retrieves the node software version.
     *
     * <p>This method acquires the read lock to ensure thread-safe access to the field.</p>
     *
     * @return the node version, or {@code null} if not set
     */
    public WorkerVersion getNodeVersion() {
        statusReadLock.lock();
        try {
            return nodeVersion;
        } finally {
            statusReadLock.unlock();
        }
    }
}
