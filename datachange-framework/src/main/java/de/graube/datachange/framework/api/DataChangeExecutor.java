package de.graube.datachange.framework.api;

import de.graube.datachange.framework.model.ChangeSetDefinition;

/**
 * Executes parsed ChangeSets.
 */
public interface DataChangeExecutor {

    DataChangeExecutionResult execute(ChangeSetDefinition changeSet);
}

