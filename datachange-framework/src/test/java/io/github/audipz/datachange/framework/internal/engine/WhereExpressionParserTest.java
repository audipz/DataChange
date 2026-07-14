package io.github.audipz.datachange.framework.internal.engine;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class WhereExpressionParserTest {

    @Test
    void parsesNestedBooleanExpressionWithMultipleConditions() {
        WhereExpressionParser.ParsedWhereClause parsed = WhereExpressionParser.parse(
                "age >= 18 and age <= 65 and (active == true or vip == true)",
                "e"
        );

        assertThat(parsed.jpql()).contains("e.age >= :p1");
        assertThat(parsed.jpql()).contains("e.age <= :p2");
        assertThat(parsed.jpql()).contains("e.active = :p3");
        assertThat(parsed.jpql()).contains("e.vip = :p4");
        assertThat(((Number) parsed.parameters().get("p1")).doubleValue()).isEqualTo(18d);
        assertThat(((Number) parsed.parameters().get("p2")).doubleValue()).isEqualTo(65d);
        assertThat(parsed.parameters()).containsEntry("p3", true);
        assertThat(parsed.parameters()).containsEntry("p4", true);
    }

    @Test
    void parsesNullComparison() {
        WhereExpressionParser.ParsedWhereClause parsed = WhereExpressionParser.parse("deletedAt == null", "e");
        assertThat(parsed.jpql()).isEqualTo("(e.deletedAt is null)");
        assertThat(parsed.parameters()).isEmpty();
    }

    @Test
    void parsesInAndNotInExpressions() {
        WhereExpressionParser.ParsedWhereClause parsed = WhereExpressionParser.parse(
                "email in ('a@test.de', 'b@test.de') and role not in ('ADMIN')",
                "e"
        );

        assertThat(parsed.jpql()).contains("e.email in (:p1, :p2)");
        assertThat(parsed.jpql()).contains("e.role not in (:p3)");
        assertThat(parsed.parameters()).containsEntry("p1", "a@test.de");
        assertThat(parsed.parameters()).containsEntry("p2", "b@test.de");
        assertThat(parsed.parameters()).containsEntry("p3", "ADMIN");
    }

    @Test
    void parsesInWithAdditionalNestedConditions() {
        WhereExpressionParser.ParsedWhereClause parsed = WhereExpressionParser.parse(
                "(email in ('a@test.de', 'b@test.de') or age >= 18) and not (active == false)",
                "e"
        );

        assertThat(parsed.jpql()).contains("e.email in (:p1, :p2)");
        assertThat(parsed.jpql()).contains("e.age >= :p3");
        assertThat(parsed.jpql()).contains("not");
        assertThat(parsed.parameters()).containsEntry("p1", "a@test.de");
        assertThat(parsed.parameters()).containsEntry("p2", "b@test.de");
    }
}
