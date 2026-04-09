package me.jianwen.mediask.infra.ai.client.mapper;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import me.jianwen.mediask.domain.ai.model.AiChatInvocation;
import me.jianwen.mediask.domain.ai.model.AiChatReply;
import me.jianwen.mediask.domain.ai.model.AiChatTriageResult;
import me.jianwen.mediask.domain.ai.model.AiCitation;
import me.jianwen.mediask.domain.ai.model.AiExecutionMetadata;
import me.jianwen.mediask.domain.ai.model.GuardrailAction;
import me.jianwen.mediask.domain.ai.model.RecommendedDepartment;
import me.jianwen.mediask.domain.ai.model.RiskLevel;
import me.jianwen.mediask.infra.ai.client.dto.PythonChatRequest;
import me.jianwen.mediask.infra.ai.client.dto.PythonChatResponse;

public final class PythonAiChatMapper {

    public PythonChatRequest toRequest(AiChatInvocation invocation) {
        return toRequest(invocation, false);
    }

    public PythonChatRequest toStreamRequest(AiChatInvocation invocation) {
        return toRequest(invocation, true);
    }

    public AiChatTriageResult toStreamMetaDomain(PythonChatResponse response) {
        return new AiChatTriageResult(
                firstNonBlank(response.chiefComplaintSummary(), response.summary()),
                toRiskLevel(response.riskLevel()),
                toGuardrailAction(response.guardrailAction()),
                mapRecommendedDepartments(response.recommendedDepartments()),
                response.careAdvice(),
                mapCitations(response.citations()),
                new AiExecutionMetadata(
                        response.providerRunId(),
                        response.matchedRuleCodes(),
                        response.tokensInput(),
                        response.tokensOutput(),
                        response.latencyMs(),
                        Boolean.TRUE.equals(response.degraded())));
    }

    public AiChatReply toDomain(PythonChatResponse response) {
        return toReply(response, true);
    }

    private PythonChatRequest toRequest(AiChatInvocation invocation, boolean stream) {
        return new PythonChatRequest(
                invocation.modelRunId(),
                invocation.turnId(),
                invocation.sessionUuid(),
                invocation.departmentId(),
                invocation.sceneType().name(),
                invocation.message(),
                invocation.contextSummary(),
                invocation.useRag(),
                stream);
    }

    private AiChatReply toReply(PythonChatResponse response, boolean answerRequired) {
        String answer = answerRequired ? requireAnswer(response.answer()) : response.answer();
        return new AiChatReply(
                answer,
                firstNonBlank(response.chiefComplaintSummary(), response.summary()),
                toRiskLevel(response.riskLevel()),
                toGuardrailAction(response.guardrailAction()),
                mapRecommendedDepartments(response.recommendedDepartments()),
                response.careAdvice(),
                mapCitations(response.citations()),
                new AiExecutionMetadata(
                        response.providerRunId(),
                        response.matchedRuleCodes(),
                        response.tokensInput(),
                        response.tokensOutput(),
                        response.latencyMs(),
                        Boolean.TRUE.equals(response.degraded())));
    }

    private List<RecommendedDepartment> mapRecommendedDepartments(
            List<PythonChatResponse.PythonRecommendedDepartment> departments) {
        if (departments == null || departments.isEmpty()) {
            return List.of();
        }
        if (departments.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("recommendedDepartments contains null element");
        }
        return departments.stream()
                .map(department -> new RecommendedDepartment(
                        department.departmentId(),
                        department.departmentName(),
                        department.priority(),
                        department.reason()))
                .toList();
    }

    private List<AiCitation> mapCitations(List<PythonChatResponse.PythonCitation> citations) {
        if (citations == null || citations.isEmpty()) {
            return List.of();
        }
        if (citations.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("citations contains null element");
        }
        return citations.stream()
                .map(citation -> new AiCitation(
                        citation.chunkId(), citation.retrievalRank(), citation.fusionScore(), citation.snippet()))
                .toList();
    }

    private RiskLevel toRiskLevel(String value) {
        return parseEnum(value, RiskLevel.class, "riskLevel");
    }

    private GuardrailAction toGuardrailAction(String value) {
        return parseEnum(value, GuardrailAction.class, "guardrailAction");
    }

    private <T extends Enum<T>> T parseEnum(String value, Class<T> enumType, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        try {
            return Enum.valueOf(enumType, value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Unsupported " + fieldName + ": " + value, exception);
        }
    }

    private String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first;
        }
        return second;
    }

    private String requireAnswer(String answer) {
        if (answer == null || answer.isBlank()) {
            throw new IllegalArgumentException("answer must not be blank");
        }
        return answer;
    }
}
