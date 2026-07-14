package io.github.audipz.datachange.framework.internal.engine;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;

import java.util.List;

/**
 * Parses and executes where-clause expressions.
 */
public final class WhereClauseParser {

    public List<?> findByWhere(String entity, String where, EntityManager entityManager) {
        if (where == null || where.isBlank()) {
            Query query = entityManager.createQuery("select e from " + entity + " e");
            return query.getResultList();
        }

        WhereExpressionParser.ParsedWhereClause parsed = WhereExpressionParser.parse(where.trim(), "e");
        Query query = entityManager.createQuery("select e from " + entity + " e where " + parsed.jpql());
        for (var entry : parsed.parameters().entrySet()) {
            query.setParameter(entry.getKey(), entry.getValue());
        }
        return query.getResultList();
    }
}
