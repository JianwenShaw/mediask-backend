package me.jianwen.mediask.infra.persistence.mapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegistrationDetailRow {

    private Long registrationId;
    private Long patientUserId;
    private String orderNo;
    private String orderStatus;
    private OffsetDateTime createdAt;
    private String sourceAiSessionId;
    private Long clinicSessionId;
    private Long clinicSlotId;
    private Long departmentId;
    private String departmentName;
    private Long doctorId;
    private String doctorName;
    private LocalDate sessionDate;
    private String periodCode;
    private BigDecimal fee;
    private OffsetDateTime cancelledAt;
    private String cancellationReason;
}
