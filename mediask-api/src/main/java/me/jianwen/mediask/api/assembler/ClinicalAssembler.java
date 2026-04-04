package me.jianwen.mediask.api.assembler;

import java.util.List;
import me.jianwen.mediask.api.dto.EncounterListItemResponse;
import me.jianwen.mediask.api.dto.EncounterListResponse;
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

    public static EncounterListResponse toEncounterListResponse(List<EncounterListItem> items) {
        return new EncounterListResponse(items.stream()
                .map(ClinicalAssembler::toEncounterListItemResponse)
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
