package io.github.audipz.datachange.framework.internal.engine;

import io.github.audipz.datachange.framework.spi.EntityResolver;
import jakarta.persistence.EntityManager;
import jakarta.persistence.metamodel.EntityType;
import org.springframework.stereotype.Component;

/**
 * Default JPA entity resolver.
 */
@Component
final class DefaultEntityResolver implements EntityResolver {

    @Override
    public EntityType<?> resolve(String entityName, EntityManager entityManager) {
        return entityManager.getMetamodel().getEntities().stream()
                .filter(it -> it.getName().equals(entityName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown entity: " + entityName));
    }
}

