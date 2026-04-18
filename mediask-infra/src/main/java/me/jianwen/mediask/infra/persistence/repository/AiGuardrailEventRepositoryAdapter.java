package me.jianwen.mediask.infra.persistence.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import me.jianwen.mediask.domain.ai.model.AiGuardrailEvent;
import me.jianwen.mediask.domain.ai.port.AiGuardrailEventRepository;
import me.jianwen.mediask.infra.persistence.dataobject.AiGuardrailEventDO;
import me.jianwen.mediask.infra.persistence.mapper.AiGuardrailEventMapper;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

@Component
public class AiGuardrailEventRepositoryAdapter implements AiGuardrailEventRepository {

    private final AiGuardrailEventMapper aiGuardrailEventMapper;
    private final ObjectMapper objectMapper;

    public AiGuardrailEventRepositoryAdapter(
            AiGuardrailEventMapper aiGuardrailEventMapper, ObjectProvider<ObjectMapper> objectMapperProvider) {
        this.aiGuardrailEventMapper = aiGuardrailEventMapper;
        this.objectMapper = objectMapperProvider.getIfAvailable(ObjectMapper::new).copy().findAndRegisterModules();
    }

    @Override
    public void save(AiGuardrailEvent aiGuardrailEvent) {
        AiGuardrailEventDO dataObject = new AiGuardrailEventDO();
        dataObject.setId(aiGuardrailEvent.id());
        dataObject.setRunId(aiGuardrailEvent.runId());
        dataObject.setRiskLevel(aiGuardrailEvent.riskLevel().name().toLowerCase());
        dataObject.setActionTaken(aiGuardrailEvent.actionTaken().name().toLowerCase());
        dataObject.setMatchedRuleCodes(writeJson(aiGuardrailEvent.matchedRuleCodes()));
        dataObject.setInputHash(aiGuardrailEvent.inputHash());
        dataObject.setOutputHash(aiGuardrailEvent.outputHash());
        dataObject.setEventDetailJson(writeJson(new AiGuardrailEventDetailPayload(
                aiGuardrailEvent.triageStage().name(),
                aiGuardrailEvent.triageCompletionReason() == null
                        ? null
                        : aiGuardrailEvent.triageCompletionReason().name(),
                aiGuardrailEvent.followUpQuestions(),
                aiGuardrailEvent.chiefComplaintSummary(),
                aiGuardrailEvent.recommendedDepartments().stream()
                        .map(department -> new RecommendedDepartmentPayload(
                                department.departmentId(),
                                department.departmentName(),
                                department.priority(),
                                department.reason()))
                        .toList(),
                aiGuardrailEvent.careAdvice())));
        aiGuardrailEventMapper.insertAiGuardrailEvent(dataObject);
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("failed to serialize ai guardrail event", exception);
        }
    }

    private record AiGuardrailEventDetailPayload(
            String triageStage,
            String triageCompletionReason,
            List<String> followUpQuestions,
            String chiefComplaintSummary,
            List<RecommendedDepartmentPayload> recommendedDepartments,
            String careAdvice) {}

    private record RecommendedDepartmentPayload(
            Long departmentId, String departmentName, Integer priority, String reason) {}
}
