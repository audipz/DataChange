package io.github.audipz.datachange.framework.spi;

import io.github.audipz.datachange.framework.model.ChangeSetDefinition;

/**
 * Serializes a {@link ChangeSetDefinition} into a stable textual representation.
 */
public interface ChangeSetSerializer {

    String serialize(ChangeSetDefinition definition);
}

