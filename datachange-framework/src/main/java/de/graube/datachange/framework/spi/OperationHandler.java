package de.graube.datachange.framework.spi;

import de.graube.datachange.framework.api.ExecutionContext;
import de.graube.datachange.framework.model.ChangeDefinition;
import jakarta.persistence.EntityManager;

/**
 * Allows custom operations to be contributed as plugins.
 */
public interface OperationHandler {

    boolean supports(String op);

    OperationOutcome execute(ChangeDefinition change, EntityManager entityManager, ExecutionContext context);
}

