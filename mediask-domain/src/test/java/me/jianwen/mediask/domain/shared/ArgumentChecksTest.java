package me.jianwen.mediask.domain.shared;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import me.jianwen.mediask.common.util.ArgumentChecks;
import org.junit.jupiter.api.Test;

class ArgumentChecksTest {

    @Test
    void requireNonBlank_WhenValueContainsWhitespace_TrimAndReturn() {
        String normalized = ArgumentChecks.requireNonBlank("  value  ", "field");

        assertEquals("value", normalized);
    }

    @Test
    void requireNonBlank_WhenValueIsBlank_ThrowException() {
        IllegalArgumentException exception =
                assertThrows(IllegalArgumentException.class, () -> ArgumentChecks.requireNonBlank("   ", "field"));

        assertEquals("field must not be blank", exception.getMessage());
    }

    @Test
    void blankToNull_WhenValueIsBlank_ReturnNull() {
        assertNull(ArgumentChecks.blankToNull("   "));
    }

    @Test
    void blankToNull_WhenValueContainsWhitespace_TrimAndReturn() {
        assertEquals("value", ArgumentChecks.blankToNull("  value  "));
    }

    @Test
    void requirePositive_WhenValueIsPositive_ReturnValue() {
        assertEquals(12L, ArgumentChecks.requirePositive(12L, "field"));
    }

    @Test
    void requirePositive_WhenValueIsZero_ThrowException() {
        IllegalArgumentException exception =
                assertThrows(IllegalArgumentException.class, () -> ArgumentChecks.requirePositive(0L, "field"));

        assertEquals("field must be greater than 0", exception.getMessage());
    }

    @Test
    void normalizePositiveLong_WhenValueIsNull_ReturnNull() {
        assertNull(ArgumentChecks.normalizePositive((Long) null, "field"));
    }

    @Test
    void normalizePositiveLong_WhenValueIsNegative_ThrowException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class, () -> ArgumentChecks.normalizePositive(-1L, "field"));

        assertEquals("field must be greater than 0", exception.getMessage());
    }

    @Test
    void normalizePositiveInteger_WhenValueIsPositive_ReturnValue() {
        assertEquals(3, ArgumentChecks.normalizePositive(3, "field"));
    }

    @Test
    void normalizePositiveInteger_WhenValueIsZero_ThrowException() {
        IllegalArgumentException exception =
                assertThrows(IllegalArgumentException.class, () -> ArgumentChecks.normalizePositive(0, "field"));

        assertEquals("field must be greater than 0", exception.getMessage());
    }
}
