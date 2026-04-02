package me.jianwen.mediask.api.assembler;

import java.util.List;
import me.jianwen.mediask.api.dto.ClinicSessionListItemResponse;
import me.jianwen.mediask.api.dto.ClinicSessionListResponse;
import me.jianwen.mediask.domain.outpatient.model.ClinicSessionListItem;

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
}
