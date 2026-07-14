package io.github.audipz.datachange.framework.internal.engine;

import io.github.audipz.datachange.framework.api.ExecutionContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Resolves placeholders in operation values.
 */
public final class OperationValueResolver {

    private static final Pattern REF_PATTERN = Pattern.compile("\\$\\{ref\\('([^']+)'\\)}");
    private static final Pattern LOOKUP_PATTERN = Pattern.compile(
            "\\$\\{lookup\\('([^']+)'\\s*,\\s*'([^']+)'\\s*,\\s*(?:'([^']*)'|([^,\\)]+))\\s*,\\s*'([^']+)'\\)}"
    );
    private static final Pattern LOOKUP_COMPLEX_PATTERN = Pattern.compile(
            "\\$\\{lookup\\('([^']+)'\\s*,\\s*\"([^\"]+)\"\\s*,\\s*'([^']+)'\\)}"
    );

    private OperationValueResolver() {
    }

    public static Object resolveValue(Object raw, ExecutionContext context, EntityManager entityManager) {
        if (raw instanceof String str) {
            Matcher refMatcher = REF_PATTERN.matcher(str);
            if (refMatcher.matches()) {
                return context.get(refMatcher.group(1));
            }

            Matcher complexMatcher = LOOKUP_COMPLEX_PATTERN.matcher(str);
            if (complexMatcher.matches()) {
                return lookupByExpression(
                        complexMatcher.group(1),
                        complexMatcher.group(2),
                        complexMatcher.group(3),
                        entityManager
                );
            }

            Matcher lookupMatcher = LOOKUP_PATTERN.matcher(str);
            if (lookupMatcher.matches()) {
                Object whereValue = toLookupValue(lookupMatcher.group(3), lookupMatcher.group(4));
                return lookup(
                        lookupMatcher.group(1),
                        lookupMatcher.group(2),
                        whereValue,
                        lookupMatcher.group(5),
                        entityManager
                );
            }
        }
        if (raw instanceof List<?> list) {
            List<Object> resolved = new ArrayList<>();
            for (Object it : list) {
                resolved.add(resolveValue(it, context, entityManager));
            }
            return resolved;
        }
        return raw;
    }

    private static Object lookup(String entity, String whereField, Object whereValue, String selectField, EntityManager entityManager) {
        Query query = entityManager.createQuery(
                "select e." + selectField + " from " + entity + " e where e." + whereField + " = :whereValue"
        );
        query.setParameter("whereValue", whereValue);
        return getSingleLookupResult(query.getResultList(), entity, whereField + " = " + formatWhereValue(whereValue));
    }

    private static Object lookupByExpression(String entity, String whereExpression, String selectField, EntityManager entityManager) {
        WhereExpressionParser.ParsedWhereClause parsed = WhereExpressionParser.parse(whereExpression, "e");
        Query query = entityManager.createQuery(
                "select e." + selectField + " from " + entity + " e where " + parsed.jpql()
        );
        for (var entry : parsed.parameters().entrySet()) {
            query.setParameter(entry.getKey(), entry.getValue());
        }
        return getSingleLookupResult(query.getResultList(), entity, whereExpression);
    }

    private static Object getSingleLookupResult(List<?> rows, String entity, String criteria) {
        if (rows.isEmpty()) {
            throw new IllegalArgumentException("Lookup returned no rows for " + entity + " where " + criteria);
        }
        if (rows.size() > 1) {
            throw new IllegalArgumentException("Lookup is ambiguous for " + entity + " where " + criteria);
        }
        return rows.getFirst();
    }

    private static Object toLookupValue(String quoted, String literalToken) {
        if (quoted != null) {
            return quoted;
        }
        String token = literalToken == null ? "" : literalToken.trim();

        if (token.equalsIgnoreCase("true")) {
            return Boolean.TRUE;
        }
        if (token.equalsIgnoreCase("false")) {
            return Boolean.FALSE;
        }
        if (token.equalsIgnoreCase("null")) {
            return null;
        }
        if (token.matches("-?\\d+")) {
            long asLong = Long.parseLong(token);
            if (asLong >= Integer.MIN_VALUE && asLong <= Integer.MAX_VALUE) {
                return (int) asLong;
            }
            return asLong;
        }
        if (token.matches("-?\\d+\\.\\d+")) {
            return Double.parseDouble(token);
        }
        return token;
    }

    private static String formatWhereValue(Object whereValue) {
        if (whereValue instanceof String) {
            return "'" + whereValue + "'";
        }
        return String.valueOf(whereValue);
    }

    public static void storeReference(String saveAs, Object entity, ExecutionContext context) {
        if (saveAs != null && !saveAs.isBlank()) {
            context.put(saveAs, entity);
        }
    }
}
