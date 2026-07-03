package io.github.audipz.datachange.framework.engine;

import tools.jackson.databind.ObjectMapper;
import io.github.audipz.datachange.framework.api.ExecutionContext;
import io.github.audipz.datachange.framework.internal.engine.OperationValueResolver;
import io.github.audipz.datachange.framework.internal.engine.PropertyApplier;
import io.github.audipz.datachange.framework.internal.engine.WhereClauseParser;
import io.github.audipz.datachange.framework.model.ChangeDefinition;
import io.github.audipz.datachange.framework.spi.EntityResolver;
import io.github.audipz.datachange.framework.spi.OperationOutcome;
import jakarta.persistence.EntityManager;
import jakarta.persistence.metamodel.EntityType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;

/**
 * Executes built-in operations via JPA EntityManager only.
 */
@Component
public class JpaOperationExecutor {

    private final ObjectMapper objectMapper;
    private final EntityResolver entityResolver;

    public JpaOperationExecutor(ObjectMapper objectMapper, EntityResolver entityResolver) {
        this.objectMapper = objectMapper;
        this.entityResolver = entityResolver;
    }

    public OperationOutcome execute(ChangeDefinition change, EntityManager entityManager, ExecutionContext context) {
        String op = change.op().toLowerCase(Locale.ROOT);
        return switch (op) {
            case "insert" -> insert(change, entityManager, context);
            case "update" -> update(change, entityManager, context);
            case "delete" -> delete(change, entityManager, context);
            case "upsert", "merge" -> upsert(change, entityManager, context);
            default -> throw new IllegalArgumentException("Unsupported operation: " + change.op());
        };
    }

    private OperationOutcome insert(ChangeDefinition change, EntityManager entityManager, ExecutionContext context) {
        Object entity = instantiate(change.entity(), entityManager);
        PropertyApplier.applyValues(entity, change.values(), context, objectMapper);
        entityManager.persist(entity);
        OperationValueResolver.storeReference(change.saveAs(), entity, context);
        return OperationOutcome.INSERT;
    }

    private OperationOutcome update(ChangeDefinition change, EntityManager entityManager, ExecutionContext context) {
        List<?> entities = findByWhere(change.entity(), change.where(), entityManager);
        if (entities.isEmpty()) {
            return OperationOutcome.NONE;
        }
        long touched = 0;
        for (Object entity : entities) {
            touched += PropertyApplier.applyValues(entity, change.set(), context, objectMapper);
        }
        return touched > 0 ? OperationOutcome.UPDATE : OperationOutcome.NONE;
    }

    private OperationOutcome delete(ChangeDefinition change, EntityManager entityManager, ExecutionContext context) {
        List<?> entities = findByWhere(change.entity(), change.where(), entityManager);
        if (entities.isEmpty()) {
            return OperationOutcome.NONE;
        }
        for (Object entity : entities) {
            entityManager.remove(entity);
        }
        return OperationOutcome.DELETE;
    }

    private OperationOutcome upsert(ChangeDefinition change, EntityManager entityManager, ExecutionContext context) {
        List<?> entities = findByWhere(change.entity(), change.where(), entityManager);
        if (entities.isEmpty()) {
            return insert(change, entityManager, context);
        }
        ChangeDefinition updateEquivalent = new ChangeDefinition(
                change.id(),
                "update",
                change.entity(),
                change.where(),
                null,
                change.set() == null ? change.values() : change.set(),
                change.saveAs()
        );
        return update(updateEquivalent, entityManager, context);
    }

    private Object instantiate(String entityName, EntityManager entityManager) {
        EntityType<?> entityType = entityResolver.resolve(entityName, entityManager);
        try {
            return entityType.getJavaType().getDeclaredConstructor().newInstance();
        } catch (Exception ex) {
            throw new IllegalStateException("Cannot instantiate entity " + entityName, ex);
        }
    }

    private List<?> findByWhere(String entity, String where, EntityManager entityManager) {
        return new WhereClauseParser().findByWhere(entity, where, entityManager);
    }
}

