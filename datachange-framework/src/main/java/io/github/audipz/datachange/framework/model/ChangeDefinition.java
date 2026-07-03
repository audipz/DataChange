package io.github.audipz.datachange.framework.model;

import java.util.Map;

/**
 * A single operation in a ChangeSet.
 */
public record ChangeDefinition(
        String id,
        String op,
        String entity,
        String where,
        Map<String, Object> values,
        Map<String, Object> set,
        String saveAs
) {
}

