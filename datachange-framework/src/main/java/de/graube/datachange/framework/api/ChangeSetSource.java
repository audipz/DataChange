package de.graube.datachange.framework.api;

import de.graube.datachange.framework.model.ChangeSetDefinition;

import java.util.List;

/**
 * Loads ChangeSets from external sources.
 */
public interface ChangeSetSource {

    List<ChangeSetDefinition> load();
}

