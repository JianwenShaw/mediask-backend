package me.jianwen.mediask.api.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

public record EncounterDetailResponse(
        @JsonSerialize(using = ToStringSerializer.class) Long encounterId,
        @JsonSerialize(using = ToStringSerializer.class) Long registrationId,
        EncounterPatientSummaryResponse patientSummary) {
}
