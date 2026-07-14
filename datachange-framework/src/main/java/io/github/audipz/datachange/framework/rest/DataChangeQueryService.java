package io.github.audipz.datachange.framework.rest;

import io.github.audipz.datachange.framework.api.DataChangeQueryResult;
import io.github.audipz.datachange.framework.internal.engine.WhereClauseParser;
import jakarta.persistence.EntityManager;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Executes read-only queries for manual REST inspection.
 */
@Component
public class DataChangeQueryService {

    private final EntityManager entityManager;

    public DataChangeQueryService(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public DataChangeQueryResult query(String entity, String fields, String where) {
        List<String> selectedFields = parseFields(fields);
        List<?> results = new WhereClauseParser().findByWhere(entity, where, entityManager);

        List<Map<String, Object>> rows = results.stream()
                .map(row -> projectRow(row, selectedFields))
                .toList();

        return new DataChangeQueryResult(entity, where, selectedFields, rows.size(), rows);
    }

    private List<String> parseFields(String fields) {
        if (fields == null || fields.isBlank()) {
            throw new IllegalArgumentException("fields is required");
        }
        List<String> selectedFields = Arrays.stream(fields.split(","))
                .map(String::trim)
                .filter(field -> !field.isBlank())
                .toList();
        if (selectedFields.isEmpty()) {
            throw new IllegalArgumentException("fields is required");
        }
        return selectedFields;
    }

    private Map<String, Object> projectRow(Object row, List<String> fields) {
        BeanWrapper wrapper = new BeanWrapperImpl(row);
        Map<String, Object> projected = new LinkedHashMap<>();
        for (String field : fields) {
            projected.put(field, wrapper.getPropertyValue(field));
        }
        return projected;
    }
}

