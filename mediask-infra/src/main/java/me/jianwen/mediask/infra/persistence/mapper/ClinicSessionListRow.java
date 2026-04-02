package me.jianwen.mediask.infra.persistence.mapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ClinicSessionListRow {

    private Long clinicSessionId;
    private Long departmentId;
    private String departmentName;
    private Long doctorId;
    private String doctorName;
    private LocalDate sessionDate;
    private String periodCode;
    private String clinicType;
    private Integer remainingCount;
    private BigDecimal fee;
}
