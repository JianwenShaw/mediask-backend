package me.jianwen.mediask.api.assembler;

import java.util.List;
import java.util.Locale;
import me.jianwen.mediask.api.dto.CreateEmrRequest;
import me.jianwen.mediask.api.dto.CreateEmrResponse;
import me.jianwen.mediask.api.dto.EmrDetailResponse;
import me.jianwen.mediask.api.dto.EncounterAiSummaryResponse;
import me.jianwen.mediask.api.dto.EncounterDetailResponse;
import me.jianwen.mediask.api.dto.EncounterListItemResponse;
import me.jianwen.mediask.api.dto.EncounterListResponse;
import me.jianwen.mediask.api.dto.EncounterPatientSummaryResponse;
import me.jianwen.mediask.application.clinical.command.CreateEmrCommand;
import me.jianwen.mediask.application.clinical.query.GetEmrDetailQuery;
import me.jianwen.mediask.application.clinical.query.GetEncounterAiSummaryQuery;
import me.jianwen.mediask.application.clinical.query.GetEncounterDetailQuery;
import me.jianwen.mediask.domain.clinical.model.EncounterAiSummary;
import me.jianwen.mediask.domain.clinical.model.EncounterDetail;
import me.jianwen.mediask.domain.clinical.model.EmrRecord;
import me.jianwen.mediask.application.clinical.query.ListEncountersQuery;
import me.jianwen.mediask.common.exception.BizException;
import me.jianwen.mediask.common.exception.ErrorCode;
import me.jianwen.mediask.domain.clinical.model.EncounterListItem;
import me.jianwen.mediask.domain.clinical.model.VisitEncounterStatus;

public final class ClinicalAssembler {

    private ClinicalAssembler() {
    }

    public static ListEncountersQuery toListEncountersQuery(Long doctorId, String status) {
        return new ListEncountersQuery(doctorId, toVisitEncounterStatus(status));
    }

    public static GetEncounterDetailQuery toGetEncounterDetailQuery(Long encounterId, Long doctorId) {
        return new GetEncounterDetailQuery(encounterId, doctorId);
    }

    public static GetEncounterAiSummaryQuery toGetEncounterAiSummaryQuery(Long encounterId, Long doctorId) {
        return new GetEncounterAiSummaryQuery(encounterId, doctorId);
    }

    public static GetEmrDetailQuery toGetEmrDetailQuery(Long encounterId) {
        return new GetEmrDetailQuery(encounterId);
    }

    public static CreateEmrCommand toCreateEmrCommand(CreateEmrRequest request, Long doctorId) {
        var diagnosisCommands = request.diagnoses().stream()
                .map(dto -> new CreateEmrCommand.EmrDiagnosisCommand(
                        dto.diagnosisType(),
                        dto.diagnosisCode(),
                        dto.diagnosisName(),
                        dto.isPrimary(),
                        dto.sortOrder()))
                .toList();
        return new CreateEmrCommand(
                request.encounterId(),
                doctorId,
                request.chiefComplaintSummary(),
                request.content(),
                diagnosisCommands);
    }

    public static CreateEmrResponse toCreateEmrResponse(me.jianwen.mediask.domain.clinical.model.EmrRecord emrRecord) {
        return new CreateEmrResponse(
                emrRecord.recordId(),
                emrRecord.recordNo(),
                emrRecord.encounterId(),
                emrRecord.recordStatus().name(),
                emrRecord.version());
    }

    public static EmrDetailResponse toEmrDetailResponse(EmrRecord emrRecord) {
        return new EmrDetailResponse(
                emrRecord.recordId(),
                emrRecord.content(),
                emrRecord.diagnoses().stream()
                        .map(diagnosis -> new EmrDetailResponse.DiagnosisResponse(
                                diagnosis.diagnosisType().name(),
                                diagnosis.diagnosisCode(),
                                diagnosis.diagnosisName(),
                                diagnosis.isPrimary(),
                                diagnosis.sortOrder()))
                        .toList());
    }

    public static EncounterListResponse toEncounterListResponse(List<EncounterListItem> items) {
        return new EncounterListResponse(items.stream()
                .map(ClinicalAssembler::toEncounterListItemResponse)
                .toList());
    }

    public static EncounterDetailResponse toEncounterDetailResponse(EncounterDetail detail) {
        return new EncounterDetailResponse(
                detail.encounterId(),
                detail.registrationId(),
                new EncounterPatientSummaryResponse(
                        detail.patientSummary().patientUserId(),
                        detail.patientSummary().patientName(),
                        detail.patientSummary().departmentId(),
                        detail.patientSummary().departmentName(),
                        detail.patientSummary().sessionDate(),
                        detail.patientSummary().periodCode(),
                        detail.patientSummary().encounterStatus().name(),
                        ApiDateTimeFormatter.format(detail.patientSummary().startedAt()),
                        ApiDateTimeFormatter.format(detail.patientSummary().endedAt())));
    }

    public static EncounterAiSummaryResponse toEncounterAiSummaryResponse(EncounterAiSummary summary) {
        return new EncounterAiSummaryResponse(
                summary.encounterId(),
                summary.sessionId(),
                summary.chiefComplaintSummary(),
                summary.structuredSummary(),
                summary.riskLevel().name().toLowerCase(Locale.ROOT),
                summary.recommendedDepartments().stream()
                        .map(department -> new EncounterAiSummaryResponse.RecommendedDepartmentResponse(
                                department.departmentId(),
                                department.departmentName(),
                                department.priority(),
                                department.reason()))
                        .toList(),
                summary.latestCitations().stream()
                        .map(citation -> new EncounterAiSummaryResponse.CitationResponse(
                                citation.chunkId(),
                                citation.retrievalRank(),
                                citation.fusionScore(),
                                citation.snippet()))
                        .toList());
    }

    private static EncounterListItemResponse toEncounterListItemResponse(EncounterListItem item) {
        return new EncounterListItemResponse(
                item.encounterId(),
                item.registrationId(),
                item.patientUserId(),
                item.patientName(),
                item.departmentId(),
                item.departmentName(),
                item.sessionDate(),
                item.periodCode(),
                item.encounterStatus().name(),
                ApiDateTimeFormatter.format(item.startedAt()),
                ApiDateTimeFormatter.format(item.endedAt()));
    }

    private static VisitEncounterStatus toVisitEncounterStatus(String status) {
        if (status == null) {
            return null;
        }
        try {
            return VisitEncounterStatus.valueOf(status);
        } catch (IllegalArgumentException exception) {
            throw new BizException(ErrorCode.INVALID_PARAMETER);
        }
    }
}
