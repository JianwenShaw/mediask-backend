package me.jianwen.mediask.infra.persistence.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import me.jianwen.mediask.common.exception.ErrorCode;
import me.jianwen.mediask.common.exception.SysException;
import me.jianwen.mediask.common.id.SnowflakeIdGenerator;
import me.jianwen.mediask.domain.ai.model.AiTriageResultSnapshot;
import me.jianwen.mediask.domain.ai.port.AiTriageResultSnapshotRepository;
import me.jianwen.mediask.infra.persistence.dataobject.AiTriageResultDO;
import me.jianwen.mediask.infra.persistence.mapper.AiTriageResultMapper;
import org.springframework.stereotype.Component;

@Component
public class AiTriageResultSnapshotRepositoryAdapter implements AiTriageResultSnapshotRepository {

    private final AiTriageResultMapper aiTriageResultMapper;
    private final ObjectMapper objectMapper;

    public AiTriageResultSnapshotRepositoryAdapter(AiTriageResultMapper aiTriageResultMapper, ObjectMapper objectMapper) {
        this.aiTriageResultMapper = aiTriageResultMapper;
        this.objectMapper = objectMapper;
    }

    @Override
    public void save(AiTriageResultSnapshot snapshot) {
        AiTriageResultDO dataObject = new AiTriageResultDO();
        dataObject.setId(SnowflakeIdGenerator.nextId());
        dataObject.setRequestId(snapshot.requestId());
        dataObject.setSessionId(snapshot.sessionId());
        dataObject.setTurnId(snapshot.turnId());
        dataObject.setQueryRunId(snapshot.queryRunId());
        dataObject.setHospitalScope(snapshot.hospitalScope());
        dataObject.setTriageStage(snapshot.triageStage());
        dataObject.setTriageCompletionReason(snapshot.triageCompletionReason());
        dataObject.setNextAction(snapshot.nextAction());
        dataObject.setRiskLevel(snapshot.riskLevel());
        dataObject.setChiefComplaintSummary(snapshot.chiefComplaintSummary());
        dataObject.setCareAdvice(snapshot.careAdvice());
        dataObject.setBlockedReason(snapshot.blockedReason());
        dataObject.setCatalogVersion(snapshot.catalogVersion());
        dataObject.setRecommendedDepartmentsJson(writeJson(snapshot.recommendedDepartments()));
        dataObject.setCitationsJson(writeJson(snapshot.citations()));
        aiTriageResultMapper.insert(dataObject);
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new SysException(ErrorCode.SYSTEM_ERROR, "failed to serialize ai triage snapshot", exception);
        }
    }
}
