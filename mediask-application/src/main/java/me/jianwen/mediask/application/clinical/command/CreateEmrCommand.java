package me.jianwen.mediask.application.clinical.command;

import me.jianwen.mediask.domain.clinical.model.EmrDiagnosis;

import java.util.List;

public record CreateEmrCommand(
        Long encounterId,
        Long doctorId,
        String chiefComplaintSummary,
        String content,
        List<EmrDiagnosisCommand> diagnoses) {

    public record EmrDiagnosisCommand(
            EmrDiagnosis.DiagnosisType diagnosisType,
            String diagnosisCode,
            String diagnosisName,
            boolean isPrimary,
            int sortOrder) {
    }

    public CreateEmrCommand {
        if (encounterId == null) {
            throw new IllegalArgumentException("encounterId cannot be null");
        }
        if (doctorId == null) {
            throw new IllegalArgumentException("doctorId cannot be null");
        }
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("content cannot be null or blank");
        }
        if (diagnoses == null) {
            throw new IllegalArgumentException("diagnoses cannot be null");
        }
    }
}