package me.jianwen.mediask.api.assembler;

import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;
import me.jianwen.mediask.api.dto.CreatePrescriptionRequest;
import me.jianwen.mediask.api.dto.CreatePrescriptionResponse;
import me.jianwen.mediask.api.dto.CreateEmrRequest;
import me.jianwen.mediask.api.dto.CreateEmrResponse;
import me.jianwen.mediask.api.dto.EmrDetailResponse;
import me.jianwen.mediask.api.dto.EncounterDetailResponse;
import me.jianwen.mediask.api.dto.EncounterListItemResponse;
import me.jianwen.mediask.api.dto.EncounterListResponse;
import me.jianwen.mediask.api.dto.EncounterPatientSummaryResponse;
import me.jianwen.mediask.api.dto.PrescriptionDetailResponse;
import me.jianwen.mediask.api.dto.UpdateEncounterStatusRequest;
import me.jianwen.mediask.api.dto.UpdateEncounterStatusResponse;
import me.jianwen.mediask.application.clinical.command.CreatePrescriptionCommand;
import me.jianwen.mediask.application.clinical.command.CreateEmrCommand;
import me.jianwen.mediask.application.clinical.command.UpdateEncounterStatusCommand;
import me.jianwen.mediask.application.clinical.query.GetEmrDetailQuery;
import me.jianwen.mediask.application.clinical.query.GetEncounterDetailQuery;
import me.jianwen.mediask.application.clinical.query.GetPrescriptionDetailQuery;
import me.jianwen.mediask.application.clinical.usecase.UpdateEncounterStatusResult;
import me.jianwen.mediask.domain.clinical.model.EncounterDetail;
import me.jianwen.mediask.domain.clinical.model.EmrRecord;
import me.jianwen.mediask.domain.clinical.model.PrescriptionOrder;
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

    public static GetEmrDetailQuery toGetEmrDetailQuery(Long encounterId) {
        return new GetEmrDetailQuery(encounterId);
    }

    public static GetPrescriptionDetailQuery toGetPrescriptionDetailQuery(Long encounterId, Long doctorId) {
        return new GetPrescriptionDetailQuery(encounterId, doctorId);
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

    public static CreatePrescriptionCommand toCreatePrescriptionCommand(CreatePrescriptionRequest request, Long doctorId) {
        List<CreatePrescriptionCommand.PrescriptionItemCommand> itemCommands = new ArrayList<>();
        for (CreatePrescriptionRequest.PrescriptionItemRequest item : request.items()) {
            itemCommands.add(new CreatePrescriptionCommand.PrescriptionItemCommand(
                    item.sortOrder(),
                    item.drugName(),
                    item.drugSpecification(),
                    item.dosageText(),
                    item.frequencyText(),
                    item.durationText(),
                    item.quantity(),
                    item.unit(),
                    item.route()));
        }
        return new CreatePrescriptionCommand(request.encounterId(), doctorId, itemCommands);
    }

    public static UpdateEncounterStatusCommand toUpdateEncounterStatusCommand(
            Long encounterId, Long doctorId, UpdateEncounterStatusRequest request) {
        if (request == null || request.action() == null) {
            throw new BizException(ErrorCode.INVALID_PARAMETER);
        }
        try {
            return new UpdateEncounterStatusCommand(
                    encounterId, doctorId, UpdateEncounterStatusCommand.Action.valueOf(request.action()));
        } catch (IllegalArgumentException exception) {
            throw new BizException(ErrorCode.INVALID_PARAMETER);
        }
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

    public static CreatePrescriptionResponse toCreatePrescriptionResponse(PrescriptionOrder prescriptionOrder) {
        return new CreatePrescriptionResponse(
                prescriptionOrder.prescriptionOrderId(),
                prescriptionOrder.encounterId(),
                prescriptionOrder.prescriptionStatus().name(),
                prescriptionOrder.items().stream()
                        .map(item -> new CreatePrescriptionResponse.PrescriptionItemResponse(
                                item.sortOrder(),
                                item.drugName(),
                                item.drugSpecification(),
                                item.dosageText(),
                                item.frequencyText(),
                                item.durationText(),
                                item.quantity(),
                                item.unit(),
                                item.route()))
                        .toList());
    }

    public static PrescriptionDetailResponse toPrescriptionDetailResponse(PrescriptionOrder prescriptionOrder) {
        return new PrescriptionDetailResponse(
                prescriptionOrder.prescriptionOrderId(),
                prescriptionOrder.encounterId(),
                prescriptionOrder.prescriptionStatus().name(),
                prescriptionOrder.items().stream()
                        .map(item -> new PrescriptionDetailResponse.PrescriptionItemResponse(
                                item.sortOrder(),
                                item.drugName(),
                                item.drugSpecification(),
                                item.dosageText(),
                                item.frequencyText(),
                                item.durationText(),
                                item.quantity(),
                                item.unit(),
                                item.route()))
                        .toList());
    }

    public static EncounterListResponse toEncounterListResponse(List<EncounterListItem> items) {
        return new EncounterListResponse(items.stream()
                .map(ClinicalAssembler::toEncounterListItemResponse)
                .toList());
    }

    public static EncounterDetailResponse toEncounterDetailResponse(EncounterDetail detail) {
        Integer age = null;
        if (detail.patientSummary().birthDate() != null && detail.patientSummary().sessionDate() != null) {
            age = Period.between(detail.patientSummary().birthDate(), detail.patientSummary().sessionDate()).getYears();
        }
        return new EncounterDetailResponse(
                detail.encounterId(),
                detail.registrationId(),
                new EncounterPatientSummaryResponse(
                        detail.patientSummary().patientUserId(),
                        detail.patientSummary().patientName(),
                        detail.patientSummary().gender(),
                        detail.patientSummary().departmentId(),
                        detail.patientSummary().departmentName(),
                        detail.patientSummary().sessionDate(),
                        detail.patientSummary().periodCode(),
                        detail.patientSummary().encounterStatus().name(),
                        detail.patientSummary().startedAt(),
                        detail.patientSummary().endedAt(),
                        age));
    }

    public static UpdateEncounterStatusResponse toUpdateEncounterStatusResponse(UpdateEncounterStatusResult result) {
        return new UpdateEncounterStatusResponse(
                result.encounterId(),
                result.encounterStatus().name(),
                result.startedAt(),
                result.endedAt());
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
                item.startedAt(),
                item.endedAt());
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
