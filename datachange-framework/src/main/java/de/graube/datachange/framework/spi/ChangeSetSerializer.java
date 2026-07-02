package de.graube.datachange.framework.spi;

import de.graube.datachange.framework.model.ChangeSetDefinition;

/**
 * Serializes a {@link ChangeSetDefinition} into a stable textual representation.
 */
public interface ChangeSetSerializer {

    String serialize(ChangeSetDefinition definition);
}

