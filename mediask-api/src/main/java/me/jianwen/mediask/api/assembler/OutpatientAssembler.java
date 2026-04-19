package me.jianwen.mediask.api.assembler;

import java.util.List;
import me.jianwen.mediask.api.dto.ClinicSessionListItemResponse;
import me.jianwen.mediask.api.dto.ClinicSessionListResponse;
import me.jianwen.mediask.api.dto.CreateRegistrationRequest;
import me.jianwen.mediask.api.dto.CreateRegistrationResponse;
import me.jianwen.mediask.api.dto.RegistrationListItemResponse;
import me.jianwen.mediask.api.dto.RegistrationListResponse;
import me.jianwen.mediask.application.outpatient.command.CreateRegistrationCommand;
import me.jianwen.mediask.application.outpatient.query.ListRegistrationsQuery;
import me.jianwen.mediask.application.outpatient.usecase.CreateRegistrationResult;
import me.jianwen.mediask.domain.outpatient.model.ClinicSessionListItem;
import me.jianwen.mediask.domain.outpatient.model.RegistrationListItem;
import me.jianwen.mediask.domain.outpatient.model.RegistrationStatus;
import me.jianwen.mediask.common.exception.BizException;
import me.jianwen.mediask.common.exception.ErrorCode;

public final class OutpatientAssembler {

    private OutpatientAssembler() {
    }

    public static ClinicSessionListResponse toClinicSessionListResponse(List<ClinicSessionListItem> items) {
        return new ClinicSessionListResponse(items.stream()
                .map(OutpatientAssembler::toClinicSessionListItemResponse)
                .toList());
    }

    public static ClinicSessionListItemResponse toClinicSessionListItemResponse(ClinicSessionListItem item) {
        return new ClinicSessionListItemResponse(
                item.clinicSessionId(),
                item.departmentId(),
                item.departmentName(),
                item.doctorId(),
                item.doctorName(),
                item.sessionDate(),
                item.periodCode().name(),
                item.clinicType().name(),
                item.remainingCount(),
                item.fee());
    }

    public static CreateRegistrationCommand toCreateRegistrationCommand(
            Long patientUserId, CreateRegistrationRequest request) {
        return new CreateRegistrationCommand(
                patientUserId, request.clinicSessionId(), request.clinicSlotId(), request.sourceAiSessionId());
    }

    public static CreateRegistrationResponse toCreateRegistrationResponse(CreateRegistrationResult result) {
        return new CreateRegistrationResponse(result.registrationId(), result.orderNo(), result.status().name());
    }

    public static ListRegistrationsQuery toListRegistrationsQuery(Long patientUserId, String status) {
        return new ListRegistrationsQuery(patientUserId, toRegistrationStatus(status));
    }

    public static RegistrationListResponse toRegistrationListResponse(List<RegistrationListItem> items) {
        return new RegistrationListResponse(items.stream()
                .map(OutpatientAssembler::toRegistrationListItemResponse)
                .toList());
    }

    private static RegistrationListItemResponse toRegistrationListItemResponse(RegistrationListItem item) {
        return new RegistrationListItemResponse(
                item.registrationId(),
                item.orderNo(),
                item.status().name(),
                ApiDateTimeFormatter.format(item.createdAt()),
                item.sourceAiSessionId());
    }

    private static RegistrationStatus toRegistrationStatus(String status) {
        if (status == null) {
            return null;
        }
        try {
            return RegistrationStatus.valueOf(status);
        } catch (IllegalArgumentException exception) {
            throw new BizException(ErrorCode.INVALID_PARAMETER);
        }
    }
}
