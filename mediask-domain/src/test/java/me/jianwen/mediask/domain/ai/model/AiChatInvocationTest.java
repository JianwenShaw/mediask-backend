package me.jianwen.mediask.domain.ai.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class AiChatInvocationTest {

    @Test
    void construct_WhenMessageBlank_ThrowException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new AiChatInvocation(1L, 2L, "sess-001", "   ", AiSceneType.PRE_CONSULTATION, null, null, true));

        assertEquals("message must not be blank", exception.getMessage());
    }

    @Test
    void construct_WhenOptionalValuesContainWhitespace_NormalizeThem() {
        AiChatInvocation invocation = new AiChatInvocation(
                1L, 2L, "  sess-001  ", "  headache  ", AiSceneType.PRE_CONSULTATION, 101L, "  summary  ", true);

        assertEquals("sess-001", invocation.sessionUuid());
        assertEquals("headache", invocation.message());
        assertEquals("summary", invocation.contextSummary());
    }
}
