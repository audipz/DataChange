package io.github.audipz.datachange.framework.api;

import java.util.List;
import java.util.Map;

/**
 * Result returned by the manual read-only query endpoint.
 */
public record DataChangeQueryResult(
        String entity,
        String where,
        List<String> fields,
        long count,
        List<Map<String, Object>> rows
) {
}

