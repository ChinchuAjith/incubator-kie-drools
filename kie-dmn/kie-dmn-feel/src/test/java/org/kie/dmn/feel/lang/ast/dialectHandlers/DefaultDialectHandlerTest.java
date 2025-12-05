package org.kie.dmn.feel.lang.ast.dialectHandlers;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Period;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class DefaultDialectHandlerTest {

    @Test
    void compareNumeric() {
        assertThat(DefaultDialectHandler.compare(BigDecimal.valueOf(1), BigDecimal.valueOf(2), (l, r) -> l.compareTo(r) < 0, () -> null, () -> null)).isTrue();
        assertThat(DefaultDialectHandler.compare(1.0, 2.0, (l, r) -> l.compareTo(r) < 0, () -> null, () -> null)).isTrue();
        assertThat(DefaultDialectHandler.compare(1, 2, (l, r) -> l.compareTo(r) > 0, () -> null, () -> null)).isFalse();
        assertThat(DefaultDialectHandler.compare(BigDecimal.valueOf(1), 2, (l, r) -> l.compareTo(r) > 0, () -> null, () -> null)).isFalse();
        assertThat(DefaultDialectHandler.compare(1, BigDecimal.valueOf(2), (l, r) -> l.compareTo(r) < 0, () -> null, () -> null)).isTrue();
        assertThat(DefaultDialectHandler.compare(BigDecimal.valueOf(1), 2.3, (l, r) -> l.compareTo(r) == 0, () -> null, () -> null)).isFalse();
        assertThat(DefaultDialectHandler.compare(1.2, BigDecimal.valueOf(1.2), (l, r) -> l.compareTo(r) == 0, () -> null, () -> null)).isTrue();
        assertThat(DefaultDialectHandler.compare(BigDecimal.valueOf(1), 0L, (l, r) -> l.compareTo(r) > 0, () -> null, () -> null)).isTrue();
        assertThat(DefaultDialectHandler.compare(10L, BigDecimal.valueOf(2), (l, r) -> l.compareTo(r) < 0, () -> null, () -> null)).isFalse();
        assertThat(DefaultDialectHandler.compare(BigInteger.valueOf(1), BigInteger.valueOf(2), (l, r) -> l.compareTo(r) == 0, () -> null, () -> null)).isFalse();
        assertThat(DefaultDialectHandler.compare(BigInteger.valueOf(1), 2, (l, r) -> l.compareTo(r) < 0, () -> null, () -> null)).isTrue();
        assertThat(DefaultDialectHandler.compare(BigInteger.valueOf(1), 2.3, (l, r) -> l.compareTo(r) == 0, () -> null, () -> null)).isFalse();

    }

    @Test
    void throwsWhenFallbackSuppliersAreNull() {
        assertThatThrownBy(() -> DefaultDialectHandler.compare(1, 2,
                (l, r) -> l.compareTo(r) < 0,
                null, () -> true))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> DefaultDialectHandler.compare(1, 2,
                (l, r) -> l.compareTo(r) < 0,
                () -> true, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void nullOperandsTriggerNullFallback() {
        assertThat(DefaultDialectHandler.compare(null, 2,
                (l, r) -> l.compareTo(r) < 0,
                () -> false, () -> true)).isFalse();

        assertThat(DefaultDialectHandler.compare(1, null,
                (l, r) -> l.compareTo(r) < 0,
                () -> true, () -> false)).isTrue();
    }

    @Test
    void compareChronoPeriod() {
        Period oneYear = Period.ofYears(1);
        Period twelveMonths = Period.ofMonths(12);
        Period sixMonths = Period.ofMonths(6);

        assertThat(DefaultDialectHandler.compare(oneYear, twelveMonths,
                (l, r) -> l.compareTo(r) == 0,
                () -> null, () -> null)).isTrue();

        assertThat(DefaultDialectHandler.compare(sixMonths, twelveMonths,
                (l, r) -> l.compareTo(r) < 0,
                () -> null, () -> null)).isTrue();

        assertThat(DefaultDialectHandler.compare(twelveMonths, sixMonths,
                (l, r) -> l.compareTo(r) > 0,
                () -> null, () -> null)).isTrue();
    }

    @Test
    void compareTemporalAccessorTime() {
        LocalTime t1 = LocalTime.of(10, 0);
        LocalTime t2 = LocalTime.of(12, 0);
        assertThat(DefaultDialectHandler.compare(t1, t2,
                (l, r) -> l.compareTo(r) < 0,
                () -> null, () -> null)).isTrue();
    }

    @Test
    void compareTemporalAccessorDateTime() {
        LocalDateTime dt1 = LocalDateTime.of(2020, 1, 1, 10, 0);
        LocalDateTime dt2 = LocalDateTime.of(2020, 1, 1, 12, 0);

        assertThat(DefaultDialectHandler.compare(dt1, dt2,
                (l, r) -> l.compareTo(r) < 0,
                () -> null, () -> null)).isTrue();
    }

    @Test
    void compareStringsAndBooleans() {
        assertThat(DefaultDialectHandler.compare("abc", "abc",
                (l, r) -> l.compareTo(r) == 0,
                () -> null, () -> null)).isTrue();

        assertThat(DefaultDialectHandler.compare("abc", "def",
                (l, r) -> l.compareTo(r) < 0,
                () -> null, () -> null)).isTrue();

        assertThat(DefaultDialectHandler.compare(true, false,
                (l, r) -> l.compareTo(r) > 0,
                () -> null, () -> null)).isTrue();
    }

    @Test
    void compareDefaultFallback() {
        Object left = new Object();
        Object right = new Object();

        // Not comparable → defaultFallback
        assertThat(DefaultDialectHandler.compare(left, right,
                (l, r) -> l.compareTo(r) == 0,
                () -> null, () -> true)).isTrue();
    }

    @Test
    void isEqualComparisons() {
        List<String> singleton = Collections.singletonList("a");
        assertThat(DefaultDialectHandler.isEqual(singleton, "a", () -> null, () -> false)).isTrue();
        assertThat(DefaultDialectHandler.isEqual("a", singleton, () -> null, () -> false)).isTrue();
        assertThat(DefaultDialectHandler.isEqual(1, 1, () -> null, () -> false)).isTrue();
        assertThat(DefaultDialectHandler.isEqual(BigDecimal.valueOf(1), BigInteger.valueOf(1), () -> null, () -> false)).isTrue();
        assertThat(DefaultDialectHandler.isEqual("abc", "abc", () -> null, () -> false)).isTrue();
        assertThat(DefaultDialectHandler.isEqual("abc", "def", () -> null, () -> false)).isFalse();
        assertThat(DefaultDialectHandler.isEqual(true, true, () -> null, () -> false)).isTrue();
        assertThat(DefaultDialectHandler.isEqual(true, false, () -> null, () -> false)).isFalse();
    }

    @Test
    void isEqualMaps() {
        Map<String, Integer> m1 = Map.of("a", 1, "b", 2);
        Map<String, Integer> m2 = Map.of("a", 1, "b", 2);
        assertThat(DefaultDialectHandler.isEqual(m1, m2, () -> null, () -> false)).isTrue();

        List<Integer> l1 = Arrays.asList(1, 2, 3);
        List<Integer> l2 = Arrays.asList(1, 2, 3);
        assertThat(DefaultDialectHandler.isEqual(l1, l2, () -> null, () -> false)).isTrue();

        Period p1 = Period.ofMonths(12);
        Period p2 = Period.ofYears(1);
        // Both equal in total months
        assertThat(DefaultDialectHandler.isEqual(p1, p2, () -> null, () -> false)).isTrue();
    }

    @Test
    void isEqualNullAndDefaultFallback() {
        assertThat(DefaultDialectHandler.isEqual(null, "x", () -> false, () -> true)).isFalse();
        assertThat(DefaultDialectHandler.isEqual("x", null, () -> true, () -> false)).isTrue();
        Object left = new Object();
        Object right = new Object();
        // Not comparable, so defaultFallback is used
        assertThat(DefaultDialectHandler.isEqual(left, right, () -> null, () -> true)).isTrue();
    }
}
