package io.github.audipz.datachange.framework.api;

import java.time.Duration;

/**
 * Summary for one executed or skipped ChangeSet.
 */
public record DataChangeExecutionResult(
        String changeSetId,
        Status status,
        long inserts,
        long updates,
        long deletes,
        Duration duration,
        String message
) {
    public enum Status {
        SUCCESS,
        SKIPPED,
        FAILED
    }
}

