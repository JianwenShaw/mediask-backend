package me.jianwen.mediask.api.dto;

import java.util.List;

public record EmrDetailResponse(Long emrRecordId, String content, List<DiagnosisResponse> diagnoses) {

    public record DiagnosisResponse(
            String diagnosisType, String diagnosisCode, String diagnosisName, boolean isPrimary, int sortOrder) {
    }
}
