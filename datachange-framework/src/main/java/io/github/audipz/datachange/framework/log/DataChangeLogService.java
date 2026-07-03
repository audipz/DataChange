package io.github.audipz.datachange.framework.log;

import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;

import java.net.InetAddress;
import java.time.Duration;
import java.time.Instant;

/**
 * Persists and checks changelog entries for idempotency.
 */
@Component
public class DataChangeLogService {

    private final EntityManager entityManager;
    private final String applicationVersion;
    private final String environment;

    public DataChangeLogService(EntityManager entityManager,
            @Value("${info.app.version:unknown}") String applicationVersion,
            @Value("${spring.profiles.active:unknown}") String environment) {
        this.entityManager = entityManager;
        this.applicationVersion = applicationVersion;
        this.environment = environment;
    }

    public boolean isExecuted(String id, String checksum) {
        DataChangeLogEntry entry = entityManager.find(DataChangeLogEntry.class, id);
        if (entry == null) {
            return false;
        }
        if (!"SUCCESS".equals(entry.getStatus())) {
            return false;
        }
        if (!entry.getChecksum().equals(checksum)) {
            throw new IllegalStateException("Checksum mismatch for changeset " + id);
        }
        return true;
    }

    public void markSuccess(String id, String checksum, String author, Instant startedAt, long inserts, long updates, long deletes) {
        DataChangeLogEntry entry = new DataChangeLogEntry();
        entry.setId(id);
        entry.setChecksum(checksum);
        entry.setAuthor(author);
        entry.setStatus("SUCCESS");
        entry.setExecutedAt(Instant.now());
        entry.setDurationMs(Duration.between(startedAt, Instant.now()).toMillis());
        entry.setHostname(resolveHostname());
        entry.setInserts(inserts);
        entry.setUpdates(updates);
        entry.setDeletes(deletes);
        entry.setApplicationVersion(applicationVersion);
        entry.setEnvironment(environment);
        entityManager.merge(entry);
    }

    public void markFailed(String id, String checksum, String author, Instant startedAt, String errorMessage) {
        DataChangeLogEntry entry = new DataChangeLogEntry();
        entry.setId(id);
        entry.setChecksum(checksum);
        entry.setAuthor(author);
        entry.setStatus("FAILED");
        entry.setExecutedAt(Instant.now());
        entry.setDurationMs(Duration.between(startedAt, Instant.now()).toMillis());
        entry.setHostname(resolveHostname());
        entry.setErrorMessage(truncate(errorMessage, 1024));
        entry.setApplicationVersion(applicationVersion);
        entry.setEnvironment(environment);
        entityManager.merge(entry);
    }

    private String truncate(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        return value.length() > maxLength ? value.substring(0, maxLength) : value;
    }

    private String resolveHostname() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (Exception ex) {
            return "unknown";
        }
    }

    @Deprecated(forRemoval = true)
    public void markSuccess(String id, String checksum, String author, Instant startedAt) {
        markSuccess(id, checksum, author, startedAt, 0, 0, 0);
    }

    @Deprecated(forRemoval = true)
    public void markFailed(String id, String checksum, String author, Instant startedAt) {
        markFailed(id, checksum, author, startedAt, null);
    }
}

