package io.github.audipz.datachange.framework.api;

import io.github.audipz.datachange.framework.model.ChangeSetDefinition;

import java.util.List;

/**
 * Loads ChangeSets from external sources.
 */
public interface ChangeSetSource {

    List<ChangeSetDefinition> load();
}

