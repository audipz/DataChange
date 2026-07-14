package io.github.audipz.datachange.framework.rest;

import io.github.audipz.datachange.framework.log.DataChangeLogEntry;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DataChangeAuditControllerTest {

    @Test
    void shouldReturnAuditDtoForDeployedChangeset() {
        EntityManager entityManager = mock(EntityManager.class);
        DataChangeLogEntry entry = new DataChangeLogEntry();
        entry.setId("seed-customers");
        entry.setChecksum("abc123");
        entry.setAuthor("dev-team");
        entry.setStatus("SUCCESS");
        entry.setExecutedAt(Instant.parse("2026-07-14T08:00:00Z"));
        entry.setDurationMs(234);
        entry.setInserts(5);
        entry.setUpdates(0);
        entry.setDeletes(0);
        entry.setApplicationVersion("0.0.12");
        entry.setEnvironment("prod");
        entry.setHostname("app-01");

        when(entityManager.find(DataChangeLogEntry.class, "seed-customers")).thenReturn(entry);

        DataChangeAuditController controller = new DataChangeAuditController(entityManager);
        DataChangeAuditController.DataChangeAuditDto dto = controller.getChangeset("seed-customers");

        assertThat(dto.id()).isEqualTo("seed-customers");
        assertThat(dto.status()).isEqualTo("SUCCESS");
        assertThat(dto.inserts()).isEqualTo(5);
        assertThat(dto.applicationVersion()).isEqualTo("0.0.12");
    }

    @Test
    void shouldReturnHistoryFilteredByStatus() {
        EntityManager entityManager = mock(EntityManager.class);
        @SuppressWarnings("unchecked")
        TypedQuery<DataChangeLogEntry> query = mock(TypedQuery.class);
        DataChangeLogEntry entry = new DataChangeLogEntry();
        entry.setId("seed-customers");
        entry.setStatus("SUCCESS");
        entry.setExecutedAt(Instant.parse("2026-07-14T08:00:00Z"));
        entry.setDurationMs(10);

        when(entityManager.createQuery(anyString(), eq(DataChangeLogEntry.class))).thenReturn(query);
        when(query.setParameter("status", "SUCCESS")).thenReturn(query);
        when(query.setMaxResults(10)).thenReturn(query);
        when(query.getResultList()).thenReturn(List.of(entry));

        DataChangeAuditController controller = new DataChangeAuditController(entityManager);
        List<DataChangeAuditController.DataChangeAuditDto> history = controller.getHistory(10, "SUCCESS");

        assertThat(history).hasSize(1);
        assertThat(history.getFirst().id()).isEqualTo("seed-customers");
        assertThat(history.getFirst().status()).isEqualTo("SUCCESS");
    }

    @Test
    void shouldRejectMissingChangeset() {
        EntityManager entityManager = mock(EntityManager.class);
        DataChangeAuditController controller = new DataChangeAuditController(entityManager);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> controller.getChangeset("missing"));
        assertThat(ex.getMessage()).contains("ChangeSet not found");
    }
}

