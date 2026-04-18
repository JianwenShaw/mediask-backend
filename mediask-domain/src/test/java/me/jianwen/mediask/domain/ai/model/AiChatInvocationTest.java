package me.jianwen.mediask.domain.ai.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.Test;

class AiChatInvocationTest {

    @Test
    void construct_WhenMessageBlank_ThrowException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new AiChatInvocation(
                        1L,
                        2L,
                        "sess-001",
                        "   ",
                        AiSceneType.PRE_CONSULTATION,
                        null,
                        "default-hospital",
                        "deptcat-v1",
                        1,
                        false,
                        null,
                        true,
                        null));

        assertEquals("message must not be blank", exception.getMessage());
    }

    @Test
    void construct_WhenOptionalValuesContainWhitespace_NormalizeThem() {
        AiChatInvocation invocation = new AiChatInvocation(
                1L,
                2L,
                "  sess-001  ",
                "  headache  ",
                AiSceneType.PRE_CONSULTATION,
                101L,
                "  default-hospital  ",
                "  deptcat-v1  ",
                1,
                false,
                "  summary  ",
                true,
                List.of(11L, 12L));

        assertEquals("sess-001", invocation.sessionUuid());
        assertEquals("headache", invocation.message());
        assertEquals("default-hospital", invocation.hospitalScope());
        assertEquals("deptcat-v1", invocation.departmentCatalogVersion());
        assertEquals("summary", invocation.contextSummary());
        assertEquals(List.of(11L, 12L), invocation.knowledgeBaseIds());
    }
}
