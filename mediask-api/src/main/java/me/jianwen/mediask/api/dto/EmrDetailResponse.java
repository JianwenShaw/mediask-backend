package me.jianwen.mediask.api.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.util.List;

public record EmrDetailResponse(
        @JsonSerialize(using = ToStringSerializer.class) Long emrRecordId,
        String content,
        List<DiagnosisResponse> diagnoses) {

    public record DiagnosisResponse(
            String diagnosisType, String diagnosisCode, String diagnosisName, boolean isPrimary, int sortOrder) {
    }
}
