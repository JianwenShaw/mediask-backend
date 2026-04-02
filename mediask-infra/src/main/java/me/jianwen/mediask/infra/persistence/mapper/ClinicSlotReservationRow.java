package me.jianwen.mediask.infra.persistence.mapper;

import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ClinicSlotReservationRow {

    private Long sessionId;
    private Long slotId;
    private Integer slotVersion;
    private Long doctorId;
    private Long departmentId;
    private BigDecimal fee;
}
