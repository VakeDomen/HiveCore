package upr.famnit.components;

import upr.famnit.authentication.VerificationStatus;

import java.time.LocalDateTime;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class NodeData {

    private final ReadWriteLock statusLock = new ReentrantReadWriteLock();
    private final Lock statusReadLock = statusLock.readLock();
    private final Lock statusWriteLock = statusLock.writeLock();

    private LocalDateTime lastPing;
    private int connectionExceptionCount;
    private String nodeName;
    private volatile VerificationStatus verificationStatus;
    private volatile String nonce;
    private volatile String tags;

    public NodeData() {
        lastPing = LocalDateTime.now();
        connectionExceptionCount = 0;
        nodeName = null;
        verificationStatus = VerificationStatus.SettingUp;
        nonce = null;
        tags = null;
    }

    public void setLastPing(LocalDateTime lastPing) {
        statusWriteLock.lock();
        this.lastPing = lastPing;
        statusWriteLock.unlock();
    }

    public void tagsTestAndSet(String tags) {
        statusReadLock.lock();
        boolean same = tags.equals(this.tags);
        statusReadLock.unlock();
        if (!same) {
            statusWriteLock.lock();
            this.tags = tags;
            statusWriteLock.unlock();
        }
    }

    public void incrementExceptionCount() {
        statusWriteLock.lock();
        this.connectionExceptionCount++;
        statusWriteLock.unlock();
    }

    public void setNodeName(String nodeName) {
        statusWriteLock.lock();
        this.nodeName = nodeName;
        statusWriteLock.unlock();
    }

    public void setVerificationStatus(VerificationStatus verificationStatus) {
        statusWriteLock.lock();
        this.verificationStatus = verificationStatus;
        statusWriteLock.unlock();
    }

    public void setNonce(String nonce) {
        statusWriteLock.lock();
        this.nonce = nonce;
        statusWriteLock.unlock();
    }

    public LocalDateTime getLastPing() {
        statusReadLock.lock();
        LocalDateTime ret = lastPing;
        statusReadLock.unlock();
        return ret;
    }

    public int getConnectionExceptionCount() {
        statusReadLock.lock();
        int ret = connectionExceptionCount;
        statusReadLock.unlock();
        return ret;
    }

    public String getNodeName() {
        statusReadLock.lock();
        String ret = nodeName;
        statusReadLock.unlock();
        return ret;
    }

    public VerificationStatus getVerificationStatus() {
        statusReadLock.lock();
        VerificationStatus ret = verificationStatus;
        statusReadLock.unlock();
        return ret;
    }

    public String getNonce() {
        statusReadLock.lock();
        String ret = nonce;
        statusReadLock.unlock();
        return ret;
    }

    public String getTags() {
        statusReadLock.lock();
        String ret = tags;
        statusReadLock.unlock();
        return ret;
    }
}
