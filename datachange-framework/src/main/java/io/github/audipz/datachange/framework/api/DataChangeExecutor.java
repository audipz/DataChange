package io.github.audipz.datachange.framework.api;

import io.github.audipz.datachange.framework.model.ChangeSetDefinition;

/**
 * Executes parsed ChangeSets.
 */
public interface DataChangeExecutor {

    DataChangeExecutionResult execute(ChangeSetDefinition changeSet);
}

