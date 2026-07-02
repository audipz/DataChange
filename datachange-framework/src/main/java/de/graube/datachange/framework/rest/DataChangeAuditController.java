package de.graube.datachange.framework.rest;

import de.graube.datachange.framework.log.DataChangeLogEntry;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

/**
 * REST API for querying DataChange audit log.
 */
@RestController
@RequestMapping("/datachange/audit")
public class DataChangeAuditController {

    private final EntityManager entityManager;

    public DataChangeAuditController(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @GetMapping("/history")
    public List<DataChangeAuditDto> getHistory(
            @RequestParam(defaultValue = "100") int limit,
            @RequestParam(required = false) String status) {
        String query = "select e from DataChangeLogEntry e";
        if (status != null && !status.isBlank()) {
            query += " where e.status = :status";
        }
        query += " order by e.executedAt desc";

        TypedQuery<DataChangeLogEntry> typedQuery = entityManager.createQuery(query, DataChangeLogEntry.class);
        if (status != null && !status.isBlank()) {
            typedQuery.setParameter("status", status);
        }
        typedQuery.setMaxResults(limit);

        return typedQuery.getResultList().stream()
                .map(DataChangeAuditDto::from)
                .toList();
    }

    @GetMapping("/changeset/{id}")
    public DataChangeAuditDto getChangeset(@PathVariable String id) {
        DataChangeLogEntry entry = entityManager.find(DataChangeLogEntry.class, id);
        if (entry == null) {
            throw new IllegalArgumentException("ChangeSet not found: " + id);
        }
        return DataChangeAuditDto.from(entry);
    }

    public record DataChangeAuditDto(
            String id,
            String checksum,
            String author,
            String status,
            Instant executedAt,
            long durationMs,
            long inserts,
            long updates,
            long deletes,
            String errorMessage,
            String applicationVersion,
            String environment,
            String hostname
    ) {
        public static DataChangeAuditDto from(DataChangeLogEntry entry) {
            return new DataChangeAuditDto(
                    entry.getId(),
                    entry.getChecksum(),
                    entry.getAuthor(),
                    entry.getStatus(),
                    entry.getExecutedAt(),
                    entry.getDurationMs(),
                    entry.getInserts(),
                    entry.getUpdates(),
                    entry.getDeletes(),
                    entry.getErrorMessage(),
                    entry.getApplicationVersion(),
                    entry.getEnvironment(),
                    entry.getHostname()
            );
        }
    }
}

