package io.github.audipz.datachange.framework.condition;

import io.github.audipz.datachange.framework.model.PreConditionDefinition;
import io.github.audipz.datachange.framework.spi.ConditionProvider;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for condition evaluator logic.
 */
class ConditionEvaluatorIntegrationTest {

    private final DefaultConditionEvaluator evaluator = new DefaultConditionEvaluator();
    private final List<ConditionProvider> providers = Collections.emptyList();

    @Test
    void evaluateLiterals() {
        assertThat(evaluator.evaluate(new PreConditionDefinition("true"), null, providers)).isTrue();
        assertThat(evaluator.evaluate(new PreConditionDefinition("false"), null, providers)).isFalse();
    }

    @Test
    void evaluateNot() {
        assertThat(evaluator.evaluate(new PreConditionDefinition("not false"), null, providers)).isTrue();
        assertThat(evaluator.evaluate(new PreConditionDefinition("not true"), null, providers)).isFalse();
    }

    @Test
    void evaluateAnd() {
        assertThat(evaluator.evaluate(new PreConditionDefinition("true and true"), null, providers)).isTrue();
        assertThat(evaluator.evaluate(new PreConditionDefinition("true and false"), null, providers)).isFalse();
        assertThat(evaluator.evaluate(new PreConditionDefinition("false and false"), null, providers)).isFalse();
    }

    @Test
    void evaluateOr() {
        assertThat(evaluator.evaluate(new PreConditionDefinition("true or false"), null, providers)).isTrue();
        assertThat(evaluator.evaluate(new PreConditionDefinition("false or false"), null, providers)).isFalse();
    }

    @Test
    void evaluateNull() {
        assertThat(evaluator.evaluate(null, null, providers)).isTrue();
        assertThat(evaluator.evaluate(new PreConditionDefinition(null), null, providers)).isTrue();
        assertThat(evaluator.evaluate(new PreConditionDefinition(""), null, providers)).isTrue();
    }

    @Test
    void evaluateComplexLogic() {
        // not false = true
        assertThat(evaluator.evaluate(new PreConditionDefinition("not false"), null, providers)).isTrue();
        // false and true = false
        assertThat(evaluator.evaluate(new PreConditionDefinition("false and true"), null, providers)).isFalse();
        // true or false = true
        assertThat(evaluator.evaluate(new PreConditionDefinition("true or false"), null, providers)).isTrue();
    }
}

