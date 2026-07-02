package de.graube.datachange.framework.log;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * Tracks executed change sets for idempotency.
 */
@Entity
@Table(name = "DATA_CHANGELOG")
public class DataChangeLogEntry {

    @Id
    @Column(name = "id", nullable = false, length = 200)
    private String id;

    @Column(name = "checksum", nullable = false, length = 128)
    private String checksum;

    @Column(name = "author", length = 200)
    private String author;

    @Column(name = "git_commit", length = 64)
    private String gitCommit;

    @Column(name = "executed_at", nullable = false)
    private Instant executedAt;

    @Column(name = "duration_ms", nullable = false)
    private long durationMs;

    @Column(name = "status", nullable = false, length = 32)
    private String status;

    @Column(name = "application_version", length = 64)
    private String applicationVersion;

    @Column(name = "environment", length = 64)
    private String environment;

    @Column(name = "hostname", length = 128)
    private String hostname;

    @Column(name = "inserts")
    private long inserts;

    @Column(name = "updates")
    private long updates;

    @Column(name = "deletes")
    private long deletes;

    @Column(name = "error_message", length = 1024)
    private String errorMessage;


    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getChecksum() {
        return checksum;
    }

    public void setChecksum(String checksum) {
        this.checksum = checksum;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getGitCommit() {
        return gitCommit;
    }

    public void setGitCommit(String gitCommit) {
        this.gitCommit = gitCommit;
    }

    public Instant getExecutedAt() {
        return executedAt;
    }

    public void setExecutedAt(Instant executedAt) {
        this.executedAt = executedAt;
    }

    public long getDurationMs() {
        return durationMs;
    }

    public void setDurationMs(long durationMs) {
        this.durationMs = durationMs;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getApplicationVersion() {
        return applicationVersion;
    }

    public void setApplicationVersion(String applicationVersion) {
        this.applicationVersion = applicationVersion;
    }

    public String getEnvironment() {
        return environment;
    }

    public void setEnvironment(String environment) {
        this.environment = environment;
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public long getInserts() {
        return inserts;
    }

    public void setInserts(long inserts) {
        this.inserts = inserts;
    }

    public long getUpdates() {
        return updates;
    }

    public void setUpdates(long updates) {
        this.updates = updates;
    }

    public long getDeletes() {
        return deletes;
    }

    public void setDeletes(long deletes) {
        this.deletes = deletes;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}

