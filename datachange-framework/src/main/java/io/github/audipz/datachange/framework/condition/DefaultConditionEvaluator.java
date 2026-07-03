package io.github.audipz.datachange.framework.condition;

import io.github.audipz.datachange.framework.model.PreConditionDefinition;
import io.github.audipz.datachange.framework.internal.condition.ConditionExpressionParser;
import io.github.audipz.datachange.framework.spi.ConditionProvider;
import io.github.audipz.datachange.framework.spi.ConditionEvaluator;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Default implementation for evaluating framework preconditions with built-in boolean logic and custom providers.
 */
@Component
public class DefaultConditionEvaluator implements ConditionEvaluator {

    public boolean evaluate(PreConditionDefinition preCondition, EntityManager entityManager, List<ConditionProvider> providers) {
        if (preCondition == null || preCondition.expression() == null || preCondition.expression().isBlank()) {
            return true;
        }

        String expression = preCondition.expression().trim();
        return evaluateExpression(expression, entityManager, providers);
    }

    private boolean evaluateExpression(String expr, EntityManager entityManager, List<ConditionProvider> providers) {
        expr = expr.trim();

        if ("true".equalsIgnoreCase(expr)) return true;
        if ("false".equalsIgnoreCase(expr)) return false;

        if (expr.startsWith("not ")) {
            return !evaluateExpression(expr.substring(4).trim(), entityManager, providers);
        }

        for (ConditionProvider provider : providers) {
            if (provider.supports(expr)) {
                return provider.evaluate(expr, entityManager);
            }
        }

        if (expr.contains(" and ")) {
            return evaluateAnd(expr, entityManager, providers);
        }
        if (expr.contains(" or ")) {
            return evaluateOr(expr, entityManager, providers);
        }

        var exists = ConditionExpressionParser.parseExists(expr);
        if (exists.isPresent()) {
            var parsed = exists.get();
            long count = countBy(parsed.entity(), parsed.field(), parsed.value(), entityManager);
            return count > 0;
        }

        var count = ConditionExpressionParser.parseCount(expr);
        if (count.isPresent()) {
            var parsed = count.get();
            long lhs = countBy(parsed.entity(), parsed.field(), parsed.value(), entityManager);
            return compare(lhs, parsed.rightHandSide(), parsed.operator());
        }

        var in = ConditionExpressionParser.parseIn(expr);
        if (in.isPresent()) {
            var parsed = in.get();
            return evaluateIn(parsed.entity(), parsed.field(), parsed.operator(), parsed.values(), entityManager);
        }

        throw new IllegalArgumentException("Unsupported precondition: " + expr);
    }

    private boolean evaluateAnd(String expr, EntityManager entityManager, List<ConditionProvider> providers) {
        String[] parts = expr.split(" and ", 2);
        return evaluateExpression(parts[0].trim(), entityManager, providers) &&
                evaluateExpression(parts[1].trim(), entityManager, providers);
    }

    private boolean evaluateOr(String expr, EntityManager entityManager, List<ConditionProvider> providers) {
        String[] parts = expr.split(" or ", 2);
        return evaluateExpression(parts[0].trim(), entityManager, providers) ||
                evaluateExpression(parts[1].trim(), entityManager, providers);
    }

    private boolean evaluateIn(String entity, String field, String op, List<String> values, EntityManager entityManager) {
        String jpql = "select count(e) from " + entity + " e where e." + field + " in :values";
        Query query = entityManager.createQuery(jpql);
        query.setParameter("values", values);
        long count = (Long) query.getSingleResult();

        if (op.contains("not")) {
            return count == 0;
        } else {
            return count > 0;
        }
    }

    private long countBy(String entity, String field, String value, EntityManager entityManager) {
        String jpql = "select count(e) from " + entity + " e";
        Query query;
        if (field != null) {
            jpql += " where e." + field + " = :value";
            query = entityManager.createQuery(jpql);
            query.setParameter("value", value);
        } else {
            query = entityManager.createQuery(jpql);
        }
        return (Long) query.getSingleResult();
    }

    private boolean compare(long lhs, long rhs, String op) {
        return switch (op) {
            case "==" -> lhs == rhs;
            case "!=" -> lhs != rhs;
            case ">" -> lhs > rhs;
            case "<" -> lhs < rhs;
            case ">=" -> lhs >= rhs;
            case "<=" -> lhs <= rhs;
            default -> throw new IllegalArgumentException("Unsupported operator: " + op);
        };
    }
}
