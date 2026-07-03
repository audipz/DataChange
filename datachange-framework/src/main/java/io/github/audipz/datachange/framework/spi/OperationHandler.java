package io.github.audipz.datachange.framework.spi;

import io.github.audipz.datachange.framework.api.ExecutionContext;
import io.github.audipz.datachange.framework.model.ChangeDefinition;
import jakarta.persistence.EntityManager;

/**
 * Allows custom operations to be contributed as plugins.
 */
public interface OperationHandler {

    boolean supports(String op);

    OperationOutcome execute(ChangeDefinition change, EntityManager entityManager, ExecutionContext context);
}

