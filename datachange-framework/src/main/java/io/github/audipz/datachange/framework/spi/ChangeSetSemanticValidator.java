package io.github.audipz.datachange.framework.spi;

import io.github.audipz.datachange.framework.model.ChangeSetDefinition;

import java.util.List;

/**
 * Extension point for semantic validation.
 */
public interface ChangeSetSemanticValidator {

    List<String> validate(ChangeSetDefinition changeSet);
}

