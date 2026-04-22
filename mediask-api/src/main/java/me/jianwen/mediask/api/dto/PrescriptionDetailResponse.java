package me.jianwen.mediask.api.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.math.BigDecimal;
import java.util.List;

public record PrescriptionDetailResponse(
        @JsonSerialize(using = ToStringSerializer.class) Long prescriptionOrderId,
        @JsonSerialize(using = ToStringSerializer.class) Long encounterId,
        String status,
        List<PrescriptionItemResponse> items) {

    public record PrescriptionItemResponse(
            int sortOrder,
            String drugName,
            String drugSpecification,
            String dosageText,
            String frequencyText,
            String durationText,
            BigDecimal quantity,
            String unit,
            String route) {
    }
}
