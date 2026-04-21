package me.jianwen.mediask.infra.persistence.repository;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Optional;
import me.jianwen.mediask.domain.ai.model.AiCitation;
import me.jianwen.mediask.domain.ai.model.RecommendedDepartment;
import me.jianwen.mediask.domain.ai.model.RiskLevel;
import me.jianwen.mediask.domain.clinical.model.EncounterAiSummary;
import me.jianwen.mediask.domain.clinical.model.EncounterDetail;
import me.jianwen.mediask.domain.clinical.model.EncounterListItem;
import me.jianwen.mediask.domain.clinical.model.EncounterPatientSummary;
import me.jianwen.mediask.domain.clinical.model.VisitEncounterStatus;
import me.jianwen.mediask.domain.clinical.port.EncounterQueryRepository;
import me.jianwen.mediask.infra.persistence.mapper.AiRunCitationRow;
import me.jianwen.mediask.infra.persistence.mapper.VisitEncounterAiSummaryRow;
import me.jianwen.mediask.infra.persistence.mapper.VisitEncounterDetailRow;
import me.jianwen.mediask.infra.persistence.mapper.VisitEncounterListRow;
import me.jianwen.mediask.infra.persistence.mapper.VisitEncounterMapper;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

@Component
public class EncounterQueryRepositoryAdapter implements EncounterQueryRepository {

    private final VisitEncounterMapper visitEncounterMapper;
    private final ObjectMapper objectMapper;

    public EncounterQueryRepositoryAdapter(
            VisitEncounterMapper visitEncounterMapper, ObjectProvider<ObjectMapper> objectMapperProvider) {
        this.visitEncounterMapper = visitEncounterMapper;
        this.objectMapper = objectMapperProvider.getIfAvailable(ObjectMapper::new).copy().findAndRegisterModules();
    }

    @Override
    public List<EncounterListItem> listByDoctorId(Long doctorId, VisitEncounterStatus status) {
        return visitEncounterMapper
                .selectDoctorEncounters(doctorId, status == null ? null : status.name())
                .stream()
                .map(this::toListItem)
                .toList();
    }

    @Override
    public Optional<EncounterDetail> findDetailByEncounterId(Long encounterId) {
        VisitEncounterDetailRow row = visitEncounterMapper.selectEncounterDetail(encounterId);
        return Optional.ofNullable(row).map(this::toDetail);
    }

    @Override
    public Optional<EncounterAiSummary> findAiSummaryByEncounterId(Long encounterId) {
        VisitEncounterAiSummaryRow row = visitEncounterMapper.selectEncounterAiSummary(encounterId);
        if (row == null) {
            return Optional.empty();
        }
        TriageSnapshotPayload snapshotPayload = parseTriageSnapshot(row.getTriageSnapshotJson());
        List<AiCitation> citations = visitEncounterMapper.selectRunCitations(row.getModelRunId()).stream()
                .map(this::toCitation)
                .toList();
        return Optional.of(new EncounterAiSummary(
                row.getEncounterId(),
                row.getSessionId(),
                snapshotPayload.chiefComplaintSummary(),
                row.getStructuredSummary(),
                RiskLevel.valueOf(row.getRiskLevel().toUpperCase()),
                snapshotPayload.recommendedDepartments().stream()
                        .map(item -> new RecommendedDepartment(
                                item.departmentId(), item.departmentName(), item.priority(), item.reason()))
                        .toList(),
                citations));
    }

    private EncounterListItem toListItem(VisitEncounterListRow row) {
        return new EncounterListItem(
                row.getEncounterId(),
                row.getRegistrationId(),
                row.getPatientUserId(),
                row.getPatientName(),
                row.getDepartmentId(),
                row.getDepartmentName(),
                row.getSessionDate(),
                row.getPeriodCode(),
                VisitEncounterStatus.valueOf(row.getEncounterStatus()),
                row.getStartedAt(),
                row.getEndedAt());
    }

    private EncounterDetail toDetail(VisitEncounterDetailRow row) {
        return new EncounterDetail(
                row.getEncounterId(),
                row.getRegistrationId(),
                row.getDoctorId(),
                new EncounterPatientSummary(
                        row.getPatientUserId(),
                        row.getPatientName(),
                        row.getGender(),
                        row.getDepartmentId(),
                        row.getDepartmentName(),
                        row.getSessionDate(),
                        row.getPeriodCode(),
                        VisitEncounterStatus.valueOf(row.getEncounterStatus()),
                        row.getStartedAt(),
                        row.getEndedAt(),
                        row.getBirthDate()));
    }

    private AiCitation toCitation(AiRunCitationRow row) {
        return new AiCitation(row.getChunkId(), row.getRetrievalRank(), row.getFusionScore(), row.getSnippet());
    }

    private TriageSnapshotPayload parseTriageSnapshot(String triageSnapshotJson) {
        if (triageSnapshotJson == null || triageSnapshotJson.isBlank()) {
            throw new IllegalStateException("missing triage snapshot json");
        }
        try {
            TriageSnapshotPayload payload = objectMapper.readValue(triageSnapshotJson, TriageSnapshotPayload.class);
            if (payload == null) {
                throw new IllegalStateException("invalid triage snapshot json");
            }
            return payload;
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("failed to deserialize triage snapshot", exception);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record RecommendedDepartmentPayload(
            Long departmentId, String departmentName, Integer priority, String reason) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record TriageSnapshotPayload(
            String chiefComplaintSummary, List<RecommendedDepartmentPayload> recommendedDepartments) {

        private TriageSnapshotPayload {
            recommendedDepartments = recommendedDepartments == null ? List.of() : List.copyOf(recommendedDepartments);
        }
    }
}
