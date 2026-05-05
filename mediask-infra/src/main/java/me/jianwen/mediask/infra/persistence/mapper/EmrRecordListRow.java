package me.jianwen.mediask.infra.persistence.mapper;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import lombok.Data;

@Data
public class EmrRecordListRow {

    private Long recordId;
    private String recordNo;
    private Long encounterId;
    private String recordStatus;
    private Long departmentId;
    private String departmentName;
    private Long doctorId;
    private String doctorName;
    private LocalDate sessionDate;
    private String chiefComplaintSummary;
    private OffsetDateTime createdAt;
}
