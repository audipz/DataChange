package de.graube.datachange.framework.spi;

import de.graube.datachange.framework.model.PreConditionDefinition;
import jakarta.persistence.EntityManager;

import java.util.List;

/**
 * Evaluates ChangeSet preconditions.
 */
public interface ConditionEvaluator {

    boolean evaluate(PreConditionDefinition preCondition, EntityManager entityManager, List<ConditionProvider> providers);
}

