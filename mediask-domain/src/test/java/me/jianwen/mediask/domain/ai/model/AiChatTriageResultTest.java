package me.jianwen.mediask.domain.ai.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class AiChatTriageResultTest {

    @Test
    void construct_WhenCollectionsProvided_CopyThemDefensively() {
        List<AiCitation> citations = new ArrayList<>();
        citations.add(new AiCitation(1L, 1, 0.9D, "snippet"));

        AiChatTriageResult triageResult = new AiChatTriageResult(
                "summary",
                RiskLevel.LOW,
                GuardrailAction.ALLOW,
                List.of(new RecommendedDepartment(1L, "Neurology", 1, null)),
                null,
                citations,
                AiExecutionMetadata.empty());

        citations.clear();

        assertEquals(1, triageResult.citations().size());
    }

    @Test
    void construct_WhenRiskLevelNull_ThrowException() {
        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> new AiChatTriageResult(
                        "summary", null, GuardrailAction.ALLOW, List.of(), null, List.of(), AiExecutionMetadata.empty()));

        assertEquals("riskLevel must not be null", exception.getMessage());
    }
}
