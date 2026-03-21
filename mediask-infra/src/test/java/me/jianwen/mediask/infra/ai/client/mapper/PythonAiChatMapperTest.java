package me.jianwen.mediask.infra.ai.client.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;
import me.jianwen.mediask.domain.ai.model.AiChatReply;
import me.jianwen.mediask.domain.ai.model.GuardrailAction;
import me.jianwen.mediask.domain.ai.model.RiskLevel;
import me.jianwen.mediask.infra.ai.client.dto.PythonChatResponse;
import org.junit.jupiter.api.Test;

class PythonAiChatMapperTest {

    private final PythonAiChatMapper mapper = new PythonAiChatMapper();

    @Test
    void toDomain_WhenPythonResponseValid_MapDomainReply() {
        PythonChatResponse response = new PythonChatResponse(
                1L,
                "provider-run-001",
                "Seek offline care soon",
                "Headache with low fever",
                null,
                List.of(new PythonChatResponse.PythonRecommendedDepartment(101L, "Neurology", 1, "Evaluate headache causes first")),
                "Seek offline care soon and avoid self-diagnosis.",
                List.of(new PythonChatResponse.PythonCitation(7003001L, 1, 0.82D, "Persistent headache with fever should be assessed offline")),
                "medium",
                "caution",
                List.of("medical_triage_only"),
                100,
                60,
                1800,
                false);

        AiChatReply reply = mapper.toDomain(response);

        assertEquals(RiskLevel.MEDIUM, reply.riskLevel());
        assertEquals(GuardrailAction.CAUTION, reply.guardrailAction());
        assertEquals("Headache with low fever", reply.chiefComplaintSummary());
        assertEquals(1, reply.recommendedDepartments().size());
        assertEquals(1, reply.citations().size());
        assertEquals("provider-run-001", reply.executionMetadata().providerRunId());
    }

    @Test
    void toDomain_WhenRiskLevelUnsupported_ThrowException() {
        PythonChatResponse response = new PythonChatResponse(
                1L, "provider-run-001", "answer", null, null, List.of(), null, List.of(), "urgent", "allow", List.of(), 1, 1, 1, false);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> mapper.toDomain(response));

        assertEquals("Unsupported riskLevel: urgent", exception.getMessage());
    }

    @Test
    void toDomain_WhenRecommendedDepartmentsContainNull_ThrowException() {
        PythonChatResponse response = new PythonChatResponse(
                1L,
                "provider-run-001",
                "answer",
                null,
                null,
                departmentsWithNullElement(),
                null,
                List.of(),
                "medium",
                "allow",
                List.of(),
                1,
                1,
                1,
                false);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> mapper.toDomain(response));

        assertEquals("recommendedDepartments contains null element", exception.getMessage());
    }

    @Test
    void toDomain_WhenCitationsContainNull_ThrowException() {
        PythonChatResponse response = new PythonChatResponse(
                1L,
                "provider-run-001",
                "answer",
                null,
                null,
                List.of(),
                null,
                citationsWithNullElement(),
                "medium",
                "allow",
                List.of(),
                1,
                1,
                1,
                false);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> mapper.toDomain(response));

        assertEquals("citations contains null element", exception.getMessage());
    }

    private List<PythonChatResponse.PythonRecommendedDepartment> departmentsWithNullElement() {
        List<PythonChatResponse.PythonRecommendedDepartment> departments = new ArrayList<>();
        departments.add(new PythonChatResponse.PythonRecommendedDepartment(101L, "Neurology", 1, "reason"));
        departments.add(null);
        return departments;
    }

    private List<PythonChatResponse.PythonCitation> citationsWithNullElement() {
        List<PythonChatResponse.PythonCitation> citations = new ArrayList<>();
        citations.add(new PythonChatResponse.PythonCitation(1L, 1, 0.1D, "snippet"));
        citations.add(null);
        return citations;
    }
}
