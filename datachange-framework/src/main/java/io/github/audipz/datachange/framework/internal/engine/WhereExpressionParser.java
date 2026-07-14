package io.github.audipz.datachange.framework.internal.engine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Parses where expressions with nested boolean logic and comparison operators.
 */
public final class WhereExpressionParser {

    private final List<Token> tokens;
    private int index = 0;
    private int paramCounter = 0;
    private final Map<String, Object> parameters = new HashMap<>();

    private WhereExpressionParser(String expression) {
        this.tokens = tokenize(expression);
    }

    public static ParsedWhereClause parse(String expression, String alias) {
        WhereExpressionParser parser = new WhereExpressionParser(expression);
        String jpql = parser.parseOr(alias);
        parser.expect(TokenType.EOF);
        return new ParsedWhereClause(jpql, parser.parameters);
    }

    private String parseOr(String alias) {
        String left = parseAnd(alias);
        while (match(TokenType.OR)) {
            String right = parseAnd(alias);
            left = "(" + left + " or " + right + ")";
        }
        return left;
    }

    private String parseAnd(String alias) {
        String left = parseNot(alias);
        while (match(TokenType.AND)) {
            String right = parseNot(alias);
            left = "(" + left + " and " + right + ")";
        }
        return left;
    }

    private String parseNot(String alias) {
        if (match(TokenType.NOT)) {
            return "(not " + parseNot(alias) + ")";
        }
        return parsePrimary(alias);
    }

    private String parsePrimary(String alias) {
        if (match(TokenType.LPAREN)) {
            String nested = parseOr(alias);
            expect(TokenType.RPAREN);
            return "(" + nested + ")";
        }
        return parseComparison(alias);
    }

    private String parseComparison(String alias) {
        Token fieldToken = expect(TokenType.IDENTIFIER);
        String field = alias + "." + fieldToken.text();

        if (match(TokenType.IN)) {
            return parseInList(field, false);
        }
        if (match(TokenType.NOT)) {
            expect(TokenType.IN);
            return parseInList(field, true);
        }

        Token operator = expect(TokenType.OP_EQ, TokenType.OP_NE, TokenType.OP_GT, TokenType.OP_GTE, TokenType.OP_LT, TokenType.OP_LTE);
        Token valueToken = expect(TokenType.STRING, TokenType.NUMBER, TokenType.BOOLEAN, TokenType.NULL);

        if (valueToken.type() == TokenType.NULL) {
            if (operator.type() == TokenType.OP_EQ) {
                return "(" + field + " is null)";
            }
            if (operator.type() == TokenType.OP_NE) {
                return "(" + field + " is not null)";
            }
            throw new IllegalArgumentException("null is only supported with == or !=");
        }

        String parameterName = "p" + (++paramCounter);
        parameters.put(parameterName, valueToken.value());
        return "(" + field + " " + toJpqlOperator(operator.type()) + " :" + parameterName + ")";
    }

    private String parseInList(String field, boolean negated) {
        expect(TokenType.LPAREN);
        List<String> placeholders = new ArrayList<>();
        do {
            Token valueToken = expect(TokenType.STRING, TokenType.NUMBER, TokenType.BOOLEAN, TokenType.NULL);
            String parameterName = "p" + (++paramCounter);
            parameters.put(parameterName, valueToken.value());
            placeholders.add(":" + parameterName);
        } while (match(TokenType.COMMA));
        expect(TokenType.RPAREN);

        if (placeholders.isEmpty()) {
            throw new IllegalArgumentException("in/not in requires at least one value");
        }

        String operator = negated ? "not in" : "in";
        return "(" + field + " " + operator + " (" + String.join(", ", placeholders) + "))";
    }

    private static String toJpqlOperator(TokenType tokenType) {
        return switch (tokenType) {
            case OP_EQ -> "=";
            case OP_NE -> "<>";
            case OP_GT -> ">";
            case OP_GTE -> ">=";
            case OP_LT -> "<";
            case OP_LTE -> "<=";
            default -> throw new IllegalArgumentException("Unsupported operator token: " + tokenType);
        };
    }

    private boolean match(TokenType tokenType) {
        if (peek().type() == tokenType) {
            index++;
            return true;
        }
        return false;
    }

    private Token expect(TokenType... tokenTypes) {
        Token token = peek();
        for (TokenType tokenType : tokenTypes) {
            if (token.type() == tokenType) {
                index++;
                return token;
            }
        }
        throw new IllegalArgumentException("Unexpected token '" + token.text() + "'");
    }

    private Token peek() {
        return tokens.get(index);
    }

    private static List<Token> tokenize(String expression) {
        List<Token> result = new ArrayList<>();
        int i = 0;
        while (i < expression.length()) {
            char c = expression.charAt(i);
            if (Character.isWhitespace(c)) {
                i++;
                continue;
            }
            if (c == '(') {
                result.add(new Token(TokenType.LPAREN, "(", null));
                i++;
                continue;
            }
            if (c == ')') {
                result.add(new Token(TokenType.RPAREN, ")", null));
                i++;
                continue;
            }
            if (c == ',') {
                result.add(new Token(TokenType.COMMA, ",", null));
                i++;
                continue;
            }
            if (c == '=' && i + 1 < expression.length() && expression.charAt(i + 1) == '=') {
                result.add(new Token(TokenType.OP_EQ, "==", null));
                i += 2;
                continue;
            }
            if (c == '!' && i + 1 < expression.length() && expression.charAt(i + 1) == '=') {
                result.add(new Token(TokenType.OP_NE, "!=", null));
                i += 2;
                continue;
            }
            if (c == '>' && i + 1 < expression.length() && expression.charAt(i + 1) == '=') {
                result.add(new Token(TokenType.OP_GTE, ">=", null));
                i += 2;
                continue;
            }
            if (c == '<' && i + 1 < expression.length() && expression.charAt(i + 1) == '=') {
                result.add(new Token(TokenType.OP_LTE, "<=", null));
                i += 2;
                continue;
            }
            if (c == '>') {
                result.add(new Token(TokenType.OP_GT, ">", null));
                i++;
                continue;
            }
            if (c == '<') {
                result.add(new Token(TokenType.OP_LT, "<", null));
                i++;
                continue;
            }
            if (c == '\'') {
                int end = expression.indexOf('\'', i + 1);
                if (end < 0) {
                    throw new IllegalArgumentException("Unterminated string literal in where expression");
                }
                String value = expression.substring(i + 1, end);
                result.add(new Token(TokenType.STRING, "'" + value + "'", value));
                i = end + 1;
                continue;
            }
            if (Character.isDigit(c) || c == '-') {
                int start = i;
                i++;
                while (i < expression.length() && (Character.isDigit(expression.charAt(i)) || expression.charAt(i) == '.')) {
                    i++;
                }
                String numeric = expression.substring(start, i);
                Object value = numeric.contains(".") ? Double.parseDouble(numeric) : Long.parseLong(numeric);
                result.add(new Token(TokenType.NUMBER, numeric, value));
                continue;
            }
            if (Character.isLetter(c) || c == '_') {
                int start = i;
                i++;
                while (i < expression.length()) {
                    char current = expression.charAt(i);
                    if (Character.isLetterOrDigit(current) || current == '_' || current == '.') {
                        i++;
                    } else {
                        break;
                    }
                }
                String identifier = expression.substring(start, i);
                String lower = identifier.toLowerCase();
                if (lower.equals("and")) {
                    result.add(new Token(TokenType.AND, identifier, null));
                } else if (lower.equals("or")) {
                    result.add(new Token(TokenType.OR, identifier, null));
                } else if (lower.equals("not")) {
                    result.add(new Token(TokenType.NOT, identifier, null));
                } else if (lower.equals("in")) {
                    result.add(new Token(TokenType.IN, identifier, null));
                } else if (lower.equals("true") || lower.equals("false")) {
                    result.add(new Token(TokenType.BOOLEAN, identifier, Boolean.parseBoolean(lower)));
                } else if (lower.equals("null")) {
                    result.add(new Token(TokenType.NULL, identifier, null));
                } else {
                    result.add(new Token(TokenType.IDENTIFIER, identifier, identifier));
                }
                continue;
            }

            throw new IllegalArgumentException("Unsupported character in where expression: " + c);
        }
        result.add(new Token(TokenType.EOF, "<eof>", null));
        return result;
    }

    public record ParsedWhereClause(String jpql, Map<String, Object> parameters) {
    }

    private record Token(TokenType type, String text, Object value) {
    }

    private enum TokenType {
        IDENTIFIER,
        STRING,
        NUMBER,
        BOOLEAN,
        NULL,
        OP_EQ,
        OP_NE,
        OP_GT,
        OP_GTE,
        OP_LT,
        OP_LTE,
        AND,
        OR,
        NOT,
        IN,
        LPAREN,
        RPAREN,
        COMMA,
        EOF
    }
}

