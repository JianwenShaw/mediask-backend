package me.jianwen.mediask.infra.persistence.repository;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import me.jianwen.mediask.domain.ai.model.AiCitation;
import me.jianwen.mediask.domain.ai.model.AiContentRole;
import me.jianwen.mediask.domain.ai.model.AiSceneType;
import me.jianwen.mediask.domain.ai.model.AiSessionDetail;
import me.jianwen.mediask.domain.ai.model.AiSessionListItem;
import me.jianwen.mediask.domain.ai.model.AiSessionMessage;
import me.jianwen.mediask.domain.ai.model.AiSessionStatus;
import me.jianwen.mediask.domain.ai.model.AiTriageCompletionReason;
import me.jianwen.mediask.domain.ai.model.AiTriageResultStatus;
import me.jianwen.mediask.domain.ai.model.AiTriageSnapshot;
import me.jianwen.mediask.domain.ai.model.AiTriageStage;
import me.jianwen.mediask.domain.ai.model.AiSessionTriageResultView;
import me.jianwen.mediask.domain.ai.model.AiSessionTurnDetail;
import me.jianwen.mediask.domain.ai.model.AiTurnStatus;
import me.jianwen.mediask.domain.ai.model.GuardrailAction;
import me.jianwen.mediask.domain.ai.model.RecommendedDepartment;
import me.jianwen.mediask.domain.ai.model.RiskLevel;
import me.jianwen.mediask.domain.ai.port.AiSessionQueryRepository;
import me.jianwen.mediask.infra.persistence.dataobject.AiSessionDO;
import me.jianwen.mediask.infra.persistence.mapper.AiRunCitationRow;
import me.jianwen.mediask.infra.persistence.mapper.AiSessionDetailRow;
import me.jianwen.mediask.infra.persistence.mapper.AiSessionMessageRow;
import me.jianwen.mediask.infra.persistence.mapper.AiSessionMapper;
import me.jianwen.mediask.infra.persistence.mapper.AiSessionQueryMapper;
import me.jianwen.mediask.infra.persistence.mapper.AiSessionTriageResultRow;
import me.jianwen.mediask.infra.persistence.mapper.AiSessionTurnRow;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

@Component
public class AiSessionQueryRepositoryAdapter implements AiSessionQueryRepository {

    private final AiSessionMapper aiSessionMapper;
    private final AiSessionQueryMapper aiSessionQueryMapper;
    private final ObjectMapper objectMapper;

    public AiSessionQueryRepositoryAdapter(
            AiSessionMapper aiSessionMapper,
            AiSessionQueryMapper aiSessionQueryMapper, ObjectProvider<ObjectMapper> objectMapperProvider) {
        this.aiSessionMapper = aiSessionMapper;
        this.aiSessionQueryMapper = aiSessionQueryMapper;
        this.objectMapper = objectMapperProvider.getIfAvailable(ObjectMapper::new).copy().findAndRegisterModules();
    }

    @Override
    public List<AiSessionListItem> listSessionsByPatientUserId(Long patientUserId) {
        return aiSessionMapper
                .selectList(Wrappers.lambdaQuery(AiSessionDO.class)
                        .eq(AiSessionDO::getPatientId, patientUserId)
                        .isNull(AiSessionDO::getDeletedAt)
                        .orderByDesc(AiSessionDO::getStartedAt, AiSessionDO::getId))
                .stream()
                .map(this::toSessionListItem)
                .toList();
    }

    @Override
    public Optional<AiSessionDetail> findSessionDetailById(Long sessionId) {
        AiSessionDetailRow sessionRow = aiSessionQueryMapper.selectSessionDetail(sessionId);
        if (sessionRow == null) {
            return Optional.empty();
        }

        List<AiSessionTurnRow> turnRows = aiSessionQueryMapper.selectSessionTurns(sessionId);
        List<AiSessionMessageRow> messageRows = aiSessionQueryMapper.selectSessionMessages(sessionId);
        Map<Long, List<AiSessionMessage>> messagesByTurnId = new LinkedHashMap<>();
        for (AiSessionMessageRow messageRow : messageRows) {
            messagesByTurnId
                    .computeIfAbsent(messageRow.getTurnId(), ignored -> new ArrayList<>())
                    .add(new AiSessionMessage(
                            AiContentRole.valueOf(messageRow.getContentRole()),
                            messageRow.getContentEncrypted(),
                            messageRow.getCreatedAt()));
        }

        List<AiSessionTurnDetail> turns = turnRows.stream()
                .map(row -> new AiSessionTurnDetail(
                        row.getTurnId(),
                        row.getTurnNo(),
                        AiTurnStatus.valueOf(row.getTurnStatus()),
                        row.getStartedAt(),
                        row.getCompletedAt(),
                        row.getErrorCode(),
                        row.getErrorMessage(),
                        messagesByTurnId.getOrDefault(row.getTurnId(), List.of())))
                .toList();

        return Optional.of(new AiSessionDetail(
                sessionRow.getSessionId(),
                sessionRow.getPatientId(),
                sessionRow.getDepartmentId(),
                AiSceneType.valueOf(sessionRow.getSceneType()),
                AiSessionStatus.valueOf(sessionRow.getSessionStatus()),
                sessionRow.getChiefComplaintSummary(),
                sessionRow.getSummary(),
                sessionRow.getStartedAt(),
                sessionRow.getEndedAt(),
                turns));
    }

    @Override
    public Optional<AiSessionTriageResultView> findLatestTriageResultBySessionId(Long sessionId) {
        AiSessionTriageResultRow row = aiSessionQueryMapper.selectLatestTriageResult(sessionId);
        if (row == null) {
            return Optional.empty();
        }
        TriageSnapshotPayload snapshotPayload = parseTriageSnapshot(row.getTriageSnapshotJson());
        EventDetailPayload latestEventDetail = parseEventDetail(row.getLatestEventDetailJson());
        List<AiCitation> citations = aiSessionQueryMapper.selectRunCitations(row.getModelRunId()).stream()
                .map(this::toCitation)
                .toList();
        boolean hasActiveCycle = row.getLatestTurnNo() != null
                && row.getFinalizedTurnNo() != null
                && row.getLatestTurnNo() > row.getFinalizedTurnNo()
                && latestEventDetail.toTriageStage() == AiTriageStage.COLLECTING;
        Integer activeCycleTurnNo = hasActiveCycle ? row.getLatestTurnNo() - row.getFinalizedTurnNo() : null;
        return Optional.of(new AiSessionTriageResultView(
                row.getSessionId(),
                row.getPatientId(),
                hasActiveCycle ? AiTriageResultStatus.UPDATING : AiTriageResultStatus.CURRENT,
                snapshotPayload.toTriageStage(),
                row.getFinalizedTurnId(),
                row.getFinalizedAt(),
                hasActiveCycle,
                activeCycleTurnNo,
                snapshotPayload.chiefComplaintSummary(),
                RiskLevel.valueOf(row.getRiskLevel().toUpperCase()),
                GuardrailAction.valueOf(row.getGuardrailAction().toUpperCase()),
                snapshotPayload.recommendedDepartments().stream()
                        .map(item -> new RecommendedDepartment(
                                item.departmentId(), item.departmentName(), item.priority(), item.reason()))
                        .toList(),
                snapshotPayload.careAdvice(),
                citations));
    }

    @Override
    public Optional<AiTriageStage> findLatestTriageStageBySessionId(Long sessionId) {
        return Optional.ofNullable(aiSessionQueryMapper.selectLatestTriageEventDetail(sessionId))
                .map(this::parseEventDetail)
                .map(EventDetailPayload::toTriageStage);
    }

    @Override
    public boolean hasAccessibleTriageSession(Long patientUserId, Long sessionId) {
        return aiSessionMapper.selectCount(Wrappers.lambdaQuery(AiSessionDO.class)
                        .eq(AiSessionDO::getId, sessionId)
                        .eq(AiSessionDO::getPatientId, patientUserId)
                        .isNull(AiSessionDO::getDeletedAt))
                > 0;
    }

    private AiCitation toCitation(AiRunCitationRow row) {
        return new AiCitation(row.getChunkId(), row.getRetrievalRank(), row.getFusionScore(), row.getSnippet());
    }

    private AiSessionListItem toSessionListItem(AiSessionDO row) {
        return new AiSessionListItem(
                row.getId(),
                row.getDepartmentId(),
                AiSceneType.valueOf(row.getSceneType()),
                AiSessionStatus.valueOf(row.getSessionStatus()),
                row.getChiefComplaintSummary(),
                row.getSummary(),
                row.getStartedAt(),
                row.getEndedAt());
    }

    private TriageSnapshotPayload parseTriageSnapshot(String triageSnapshotJson) {
        if (triageSnapshotJson == null || triageSnapshotJson.isBlank()) {
            throw new IllegalStateException("missing triage snapshot json");
        }
        try {
            TriageSnapshotPayload payload = objectMapper.readValue(triageSnapshotJson, TriageSnapshotPayload.class);
            if (payload == null || payload.triageStage() == null) {
                throw new IllegalStateException("invalid triage snapshot json");
            }
            return payload;
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("failed to deserialize triage snapshot", exception);
        }
    }

    private EventDetailPayload parseEventDetail(String eventDetailJson) {
        if (eventDetailJson == null || eventDetailJson.isBlank()) {
            return EventDetailPayload.empty();
        }
        try {
            EventDetailPayload payload = objectMapper.readValue(eventDetailJson, EventDetailPayload.class);
            return payload == null ? EventDetailPayload.empty() : payload;
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("failed to deserialize ai guardrail event detail", exception);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record EventDetailPayload(
            String triageStage,
            String triageCompletionReason,
            List<String> followUpQuestions,
            String chiefComplaintSummary,
            List<RecommendedDepartmentPayload> recommendedDepartments,
            String careAdvice) {

        private EventDetailPayload {
            followUpQuestions = followUpQuestions == null ? List.of() : List.copyOf(followUpQuestions);
            recommendedDepartments = recommendedDepartments == null ? List.of() : List.copyOf(recommendedDepartments);
        }

        private static EventDetailPayload empty() {
            return new EventDetailPayload(null, null, List.of(), null, List.of(), null);
        }

        private AiTriageStage toTriageStage() {
            return triageStage == null || triageStage.isBlank() ? null : AiTriageStage.valueOf(triageStage);
        }

        @SuppressWarnings("unused")
        private AiTriageCompletionReason toTriageCompletionReason() {
            return triageCompletionReason == null || triageCompletionReason.isBlank()
                    ? null
                    : AiTriageCompletionReason.valueOf(triageCompletionReason);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record RecommendedDepartmentPayload(
            Long departmentId, String departmentName, Integer priority, String reason) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record TriageSnapshotPayload(
            String triageStage,
            String triageCompletionReason,
            String chiefComplaintSummary,
            List<RecommendedDepartmentPayload> recommendedDepartments,
            String careAdvice) {

        private TriageSnapshotPayload {
            recommendedDepartments = recommendedDepartments == null ? List.of() : List.copyOf(recommendedDepartments);
        }

        private AiTriageStage toTriageStage() {
            return AiTriageStage.valueOf(triageStage);
        }
    }
}
