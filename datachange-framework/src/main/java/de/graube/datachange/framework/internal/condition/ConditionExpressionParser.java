package de.graube.datachange.framework.internal.condition;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses built-in condition expressions for the default evaluator.
 */
public final class ConditionExpressionParser {

    private static final Pattern EXISTS_PATTERN = Pattern.compile("exists\\((\\w+)(?: where (\\w+)='([^']*)')?\\)");
    private static final Pattern COUNT_PATTERN = Pattern.compile("count\\((\\w+)(?: where (\\w+)='([^']*)')?\\)\\s*(==|!=|>=|<=|>|<)\\s*(\\d+)");
    private static final Pattern IN_PATTERN = Pattern.compile("(\\w+)\\.(\\w+)\\s+(in|not\\s+in)\\s*\\(([^)]+)\\)");

    private ConditionExpressionParser() {
    }

    public static Optional<ExistsExpression> parseExists(String expr) {
        Matcher matcher = EXISTS_PATTERN.matcher(expr);
        return matcher.find()
                ? Optional.of(new ExistsExpression(matcher.group(1), matcher.group(2), matcher.group(3)))
                : Optional.empty();
    }

    public static Optional<CountExpression> parseCount(String expr) {
        Matcher matcher = COUNT_PATTERN.matcher(expr);
        return matcher.find()
                ? Optional.of(new CountExpression(matcher.group(1), matcher.group(2), matcher.group(3), matcher.group(4), Long.parseLong(matcher.group(5))))
                : Optional.empty();
    }

    public static Optional<InExpression> parseIn(String expr) {
        Matcher matcher = IN_PATTERN.matcher(expr);
        if (!matcher.find()) {
            return Optional.empty();
        }
        List<String> values = new ArrayList<>();
        for (String v : matcher.group(4).split(",")) {
            values.add(v.trim().replaceAll("['\"]", ""));
        }
        return Optional.of(new InExpression(matcher.group(1), matcher.group(2), matcher.group(3), values));
    }

    public record ExistsExpression(String entity, String field, String value) {
    }

    public record CountExpression(String entity, String field, String value, String operator, long rightHandSide) {
    }

    public record InExpression(String entity, String field, String operator, List<String> values) {
    }
}

