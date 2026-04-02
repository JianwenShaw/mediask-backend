package me.jianwen.mediask.domain.outpatient.model;

import java.math.BigDecimal;

public record ClinicSlotReservation(
        Long sessionId, Long slotId, Long doctorId, Long departmentId, BigDecimal fee) {}
