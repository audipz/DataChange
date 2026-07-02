package de.graube.datachange.framework.internal.condition;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConditionExpressionParserTest {

    @Test
    void shouldParseExistsExpression() {
        var parsed = ConditionExpressionParser.parseExists("exists(Customer where email='demo@test.de')");

        assertTrue(parsed.isPresent());
        assertEquals("Customer", parsed.get().entity());
        assertEquals("email", parsed.get().field());
        assertEquals("demo@test.de", parsed.get().value());
    }

    @Test
    void shouldParseCountExpression() {
        var parsed = ConditionExpressionParser.parseCount("count(Customer where status='ACTIVE') >= 1");

        assertTrue(parsed.isPresent());
        assertEquals("Customer", parsed.get().entity());
        assertEquals("status", parsed.get().field());
        assertEquals("ACTIVE", parsed.get().value());
        assertEquals(">=", parsed.get().operator());
        assertEquals(1L, parsed.get().rightHandSide());
    }

    @Test
    void shouldParseInExpression() {
        var parsed = ConditionExpressionParser.parseIn("Customer.status in ('ACTIVE', 'PENDING')");

        assertTrue(parsed.isPresent());
        assertEquals("Customer", parsed.get().entity());
        assertEquals("status", parsed.get().field());
        assertEquals("in", parsed.get().operator().toLowerCase());
        assertEquals(2, parsed.get().values().size());
    }
}

