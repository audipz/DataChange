package de.graube.datachange.framework.spi;

import de.graube.datachange.framework.model.ChangeSetDefinition;

import java.util.List;

/**
 * Extension point for semantic validation.
 */
public interface ChangeSetSemanticValidator {

    List<String> validate(ChangeSetDefinition changeSet);
}

