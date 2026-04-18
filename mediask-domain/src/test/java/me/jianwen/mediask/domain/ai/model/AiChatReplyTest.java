package me.jianwen.mediask.domain.ai.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class AiChatReplyTest {

    @Test
    void construct_WhenCollectionsProvided_CopyThemDefensively() {
        List<AiCitation> citations = new ArrayList<>();
        citations.add(new AiCitation(1L, 1, 0.9D, "snippet"));

        AiChatReply reply = new AiChatReply(
                "answer",
                AiTriageStage.READY,
                AiTriageCompletionReason.SUFFICIENT_INFO,
                "summary",
                RiskLevel.LOW,
                GuardrailAction.ALLOW,
                List.of(),
                List.of(new RecommendedDepartment(1L, "Neurology", 1, null)),
                null,
                citations,
                AiExecutionMetadata.empty());

        citations.clear();

        assertEquals(1, reply.citations().size());
    }

    @Test
    void construct_WhenAnswerBlank_ThrowException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new AiChatReply(
                        "  ",
                        AiTriageStage.COLLECTING,
                        null,
                        null,
                        RiskLevel.LOW,
                        GuardrailAction.ALLOW,
                        List.of(),
                        List.of(),
                        null,
                        List.of(),
                        AiExecutionMetadata.empty()));

        assertEquals("answer must not be blank", exception.getMessage());
    }
}
