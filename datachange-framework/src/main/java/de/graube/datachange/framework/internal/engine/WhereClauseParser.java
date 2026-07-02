package de.graube.datachange.framework.internal.engine;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses and executes the built-in where-clause syntax.
 */
public final class WhereClauseParser {

    private static final Pattern WHERE_PATTERN = Pattern.compile("(\\w+)\\s*(==|=)\\s*'([^']*)'");

    public List<?> findByWhere(String entity, String where, EntityManager entityManager) {
        if (where == null || where.isBlank()) {
            Query query = entityManager.createQuery("select e from " + entity + " e");
            return query.getResultList();
        }

        Matcher matcher = WHERE_PATTERN.matcher(where.trim());
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Unsupported where expression: " + where);
        }
        String field = matcher.group(1);
        String value = matcher.group(3);

        Query query = entityManager.createQuery("select e from " + entity + " e where e." + field + " = :value");
        query.setParameter("value", value);
        return query.getResultList();
    }
}

