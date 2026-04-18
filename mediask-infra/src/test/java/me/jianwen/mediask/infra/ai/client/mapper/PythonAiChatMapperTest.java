package me.jianwen.mediask.infra.ai.client.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;
import me.jianwen.mediask.domain.ai.model.AiChatReply;
import me.jianwen.mediask.domain.ai.model.AiChatInvocation;
import me.jianwen.mediask.domain.ai.model.AiChatTriageResult;
import me.jianwen.mediask.domain.ai.model.AiTriageCompletionReason;
import me.jianwen.mediask.domain.ai.model.AiTriageStage;
import me.jianwen.mediask.domain.ai.model.AiSceneType;
import me.jianwen.mediask.domain.ai.model.GuardrailAction;
import me.jianwen.mediask.domain.ai.model.RiskLevel;
import me.jianwen.mediask.infra.ai.client.dto.PythonChatRequest;
import me.jianwen.mediask.infra.ai.client.dto.PythonChatResponse;
import org.junit.jupiter.api.Test;

class PythonAiChatMapperTest {

    private final PythonAiChatMapper mapper = new PythonAiChatMapper();

    @Test
    void toDomain_WhenPythonResponseValid_MapDomainReply() {
        PythonChatResponse response = new PythonChatResponse(
                1L,
                2L,
                "session-uuid",
                "provider-run-001",
                "Seek offline care soon",
                "Headache with low fever",
                "READY",
                "SUFFICIENT_INFO",
                null,
                List.of(),
                List.of(),
                List.of(),
                "high",
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

        assertEquals(AiTriageStage.READY, reply.triageStage());
        assertEquals(AiTriageCompletionReason.SUFFICIENT_INFO, reply.triageCompletionReason());
        assertEquals(RiskLevel.MEDIUM, reply.riskLevel());
        assertEquals(GuardrailAction.CAUTION, reply.guardrailAction());
        assertEquals("Headache with low fever", reply.chiefComplaintSummary());
        assertEquals(1, reply.recommendedDepartments().size());
        assertEquals(1, reply.citations().size());
        assertEquals("provider-run-001", reply.executionMetadata().providerRunId());
    }

    @Test
    void toStreamMetaDomain_WhenAnswerMissing_MapStructuredReply() {
        PythonChatResponse response = new PythonChatResponse(
                1L,
                2L,
                "session-uuid",
                "provider-run-001",
                null,
                "Headache with low fever",
                "COLLECTING",
                null,
                null,
                List.of("How long has the fever lasted?"),
                List.of(),
                List.of(),
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

        AiChatTriageResult triageResult = mapper.toStreamMetaDomain(response);

        assertEquals(AiTriageStage.COLLECTING, triageResult.triageStage());
        assertEquals(RiskLevel.MEDIUM, triageResult.riskLevel());
        assertEquals(GuardrailAction.CAUTION, triageResult.guardrailAction());
        assertEquals("Headache with low fever", triageResult.chiefComplaintSummary());
        assertEquals(1, triageResult.followUpQuestions().size());
        assertEquals(1, triageResult.citations().size());
        assertEquals("provider-run-001", triageResult.executionMetadata().providerRunId());
    }

    @Test
    void toDomain_WhenRiskLevelUnsupported_ThrowException() {
        PythonChatResponse response = new PythonChatResponse(
                1L, 2L, "session-uuid", "provider-run-001", "answer", null, "READY", null, null, List.of(), List.of(), List.of(), null, List.of(), null, List.of(), "urgent", "allow", List.of(), 1, 1, 1, false);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> mapper.toDomain(response));

        assertEquals("Unsupported riskLevel: urgent", exception.getMessage());
    }

    @Test
    void toDomain_WhenRecommendedDepartmentsContainNull_ThrowException() {
        PythonChatResponse response = new PythonChatResponse(
                1L,
                2L,
                "session-uuid",
                "provider-run-001",
                "answer",
                null,
                "READY",
                "SUFFICIENT_INFO",
                null,
                List.of(),
                List.of(),
                List.of(),
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
                2L,
                "session-uuid",
                "provider-run-001",
                "answer",
                null,
                "READY",
                "SUFFICIENT_INFO",
                null,
                List.of(),
                List.of(),
                List.of(),
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

    @Test
    void toRequest_WhenInvocationValid_MapRequestWithoutStreamField() {
        PythonChatRequest request = mapper.toRequest(invocation());

        assertEquals(1L, request.modelRunId());
        assertEquals(2L, request.turnId());
        assertEquals("session-uuid", request.sessionUuid());
        assertEquals(101L, request.departmentId());
        assertEquals("default-hospital", request.hospitalScope());
        assertEquals("deptcat-v1", request.departmentCatalogVersion());
        assertEquals(1, request.patientTurnNoInActiveCycle());
        assertEquals(false, request.forceFinalize());
        assertEquals("PRE_CONSULTATION", request.sceneType());
        assertEquals("头痛三天", request.message());
        assertEquals(true, request.useRag());
        assertEquals(List.of(), request.knowledgeBaseIds());
    }

    @Test
    void toRequest_WhenRagDisabled_OmitKnowledgeBaseIds() {
        PythonChatRequest request = mapper.toRequest(new AiChatInvocation(
                1L,
                2L,
                "session-uuid",
                "头痛三天",
                AiSceneType.PRE_CONSULTATION,
                101L,
                "default-hospital",
                "deptcat-v1",
                1,
                false,
                null,
                false,
                null));

        assertEquals(false, request.useRag());
        assertEquals(null, request.knowledgeBaseIds());
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

    private AiChatInvocation invocation() {
        return new AiChatInvocation(
                1L,
                2L,
                "session-uuid",
                "头痛三天",
                AiSceneType.PRE_CONSULTATION,
                101L,
                "default-hospital",
                "deptcat-v1",
                1,
                false,
                null,
                true,
                List.of());
    }
}
