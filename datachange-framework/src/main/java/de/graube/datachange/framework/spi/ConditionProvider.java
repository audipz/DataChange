package de.graube.datachange.framework.spi;

import jakarta.persistence.EntityManager;

/**
 * Optional extension point for custom condition keywords.
 */
public interface ConditionProvider {

    boolean supports(String expression);

    boolean evaluate(String expression, EntityManager entityManager);
}

