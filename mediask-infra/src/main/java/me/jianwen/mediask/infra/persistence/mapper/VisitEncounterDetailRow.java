package me.jianwen.mediask.infra.persistence.mapper;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VisitEncounterDetailRow {

    private Long encounterId;
    private Long registrationId;
    private Long doctorId;
    private Long patientUserId;
    private String patientName;
    private Long departmentId;
    private String departmentName;
    private LocalDate sessionDate;
    private String periodCode;
    private String encounterStatus;
    private OffsetDateTime startedAt;
    private OffsetDateTime endedAt;
}
