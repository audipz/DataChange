package io.github.audipz.datachange.framework.model;

import java.util.List;

/**
 * Root object for one declarative data migration.
 */
public record ChangeSetDefinition(
        String specVersion,
        String id,
        String author,
        String description,
        List<String> labels,
        List<String> tags,
        TransactionMode transactionMode,
        PreConditionDefinition preConditions,
        List<ChangeDefinition> changes
) {
}

