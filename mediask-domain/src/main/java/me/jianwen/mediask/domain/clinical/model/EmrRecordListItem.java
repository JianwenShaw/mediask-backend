package me.jianwen.mediask.domain.clinical.model;

import java.time.LocalDate;
import java.time.OffsetDateTime;

public record EmrRecordListItem(
        Long recordId,
        String recordNo,
        Long encounterId,
        EmrRecordStatus recordStatus,
        Long departmentId,
        String departmentName,
        Long doctorId,
        String doctorName,
        LocalDate sessionDate,
        String chiefComplaintSummary,
        OffsetDateTime createdAt) {
}
