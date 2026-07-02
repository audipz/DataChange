package de.graube.datachange.framework.log;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import jakarta.persistence.EntityManager;

class DataChangeLogServiceTest {

    @Test
    void shouldMarkSuccessWithStatistics() {
        EntityManager em = mock(EntityManager.class);
        DataChangeLogService service = new DataChangeLogService(em, "1.0.0", "prod");

        service.markSuccess("cs-001", "abc123", "author", Instant.now(), 5, 2, 1);

        verify(em).merge(argThat(entry -> {
            if (!(entry instanceof DataChangeLogEntry e)) return false;
            return e.getId().equals("cs-001") &&
                    e.getStatus().equals("SUCCESS") &&
                    e.getInserts() == 5 &&
                    e.getUpdates() == 2 &&
                    e.getDeletes() == 1 &&
                    e.getApplicationVersion().equals("1.0.0") &&
                    e.getEnvironment().equals("prod");
        }));
    }

    @Test
    void shouldMarkFailedWithErrorMessage() {
        EntityManager em = mock(EntityManager.class);
        DataChangeLogService service = new DataChangeLogService(em, "1.0.0", "prod");

        service.markFailed("cs-001", "abc123", "author", Instant.now(), "NullPointerException: field is null");

        verify(em).merge(argThat(entry -> {
            if (!(entry instanceof DataChangeLogEntry e)) return false;
            return e.getId().equals("cs-001") &&
                    e.getStatus().equals("FAILED") &&
                    e.getErrorMessage() != null &&
                    e.getErrorMessage().contains("NullPointerException");
        }));
    }

    @Test
    void shouldTruncateLongErrorMessage() {
        EntityManager em = mock(EntityManager.class);
        DataChangeLogService service = new DataChangeLogService(em, "1.0.0", "prod");
        String longError = "a".repeat(2000);

        service.markFailed("cs-001", "abc123", "author", Instant.now(), longError);

        verify(em).merge(argThat(entry -> {
            if (!(entry instanceof DataChangeLogEntry e)) return false;
            String msg = e.getErrorMessage();
            return msg != null && msg.length() <= 1024;
        }));
    }

    @Test
    void shouldDetectAlreadyExecutedChangeSet() {
        EntityManager em = mock(EntityManager.class);
        DataChangeLogEntry existing = new DataChangeLogEntry();
        existing.setStatus("SUCCESS");
        existing.setChecksum("abc123");
        
        when(em.find(DataChangeLogEntry.class, "cs-001")).thenReturn(existing);

        DataChangeLogService service = new DataChangeLogService(em, "1.0.0", "prod");

        boolean result = service.isExecuted("cs-001", "abc123");

        assertEquals(true, result);
    }

    @Test
    void shouldRejectChecksumMismatch() {
        EntityManager em = mock(EntityManager.class);
        DataChangeLogEntry existing = new DataChangeLogEntry();
        existing.setStatus("SUCCESS");
        existing.setChecksum("old_checksum");
        
        when(em.find(DataChangeLogEntry.class, "cs-001")).thenReturn(existing);

        DataChangeLogService service = new DataChangeLogService(em, "1.0.0", "prod");

        try {
            service.isExecuted("cs-001", "new_checksum");
            throw new AssertionError("Expected IllegalStateException");
        } catch (IllegalStateException ex) {
            assertEquals(true, ex.getMessage().contains("Checksum mismatch"));
        }
    }
}

