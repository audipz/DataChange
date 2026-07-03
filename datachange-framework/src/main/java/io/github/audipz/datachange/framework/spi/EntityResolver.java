package io.github.audipz.datachange.framework.spi;

import jakarta.persistence.EntityManager;
import jakarta.persistence.metamodel.EntityType;

/**
 * Resolves a logical entity name to a JPA entity type.
 */
public interface EntityResolver {

    EntityType<?> resolve(String entityName, EntityManager entityManager);
}

