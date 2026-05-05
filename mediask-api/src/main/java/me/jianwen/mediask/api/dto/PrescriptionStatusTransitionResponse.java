package me.jianwen.mediask.api.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

public record PrescriptionStatusTransitionResponse(
        @JsonSerialize(using = ToStringSerializer.class) Long prescriptionOrderId,
        @JsonSerialize(using = ToStringSerializer.class) Long encounterId,
        String status,
        int version) {
}
