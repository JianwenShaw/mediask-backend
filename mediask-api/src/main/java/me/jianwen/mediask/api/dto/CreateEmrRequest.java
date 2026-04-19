package me.jianwen.mediask.api.dto;

import me.jianwen.mediask.common.util.ArgumentChecks;
import me.jianwen.mediask.domain.clinical.model.EmrDiagnosis;

import java.util.List;

public record CreateEmrRequest(
        Long encounterId,
        String chiefComplaintSummary,
        String content,
        List<EmrDiagnosisRequest> diagnoses) {

    public CreateEmrRequest {
        encounterId = ArgumentChecks.requireNonNull(encounterId, "encounterId");
        content = ArgumentChecks.requireNonBlank(content, "content");
        diagnoses = ArgumentChecks.requireNonNull(diagnoses, "diagnoses");
        chiefComplaintSummary = ArgumentChecks.blankToNull(chiefComplaintSummary);
    }

    public record EmrDiagnosisRequest(
            EmrDiagnosis.DiagnosisType diagnosisType,
            String diagnosisCode,
            String diagnosisName,
            boolean isPrimary,
            int sortOrder) {

        public EmrDiagnosisRequest {
            ArgumentChecks.requireNonNull(diagnosisType, "diagnosisType");
            diagnosisName = ArgumentChecks.requireNonBlank(diagnosisName, "diagnosisName");
            diagnosisCode = ArgumentChecks.blankToNull(diagnosisCode);
            if (sortOrder < 0) {
                throw new IllegalArgumentException("sortOrder cannot be negative");
            }
        }
    }
}