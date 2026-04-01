package me.jianwen.mediask.infra.persistence.mapper;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminPatientRow {

    private Long patientId;
    private Long userId;
    private Integer userVersion;
    private Integer patientProfileVersion;
    private OffsetDateTime createdAt;
    private String patientNo;
    private String username;
    private String displayName;
    private String mobileMasked;
    private String gender;
    private LocalDate birthDate;
    private String bloodType;
    private String allergySummary;
    private String accountStatus;
}
