package org.kie.dmn.feel.lang.ast.dialectHandlers;

import java.util.Map;
import java.util.function.BiFunction;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class BFeelDialectHandlerTest {

    @Test
    void stringOperandsConcatenate() {
        BFEELDialectHandler handler = new BFEELDialectHandler();
        Map<DefaultDialectHandler.CheckedPredicate, BiFunction<Object, Object, Object>> ops = handler.getAddOperations(null);

        // Find the string concatenation operation
        var entry = ops.entrySet().stream()
                .filter(e -> e.getKey().predicate.test("a", 123))
                .findFirst()
                .orElseThrow();

        Object result = entry.getValue().apply("a", 123);
        assertThat(result).isEqualTo("a123");
    }
}
