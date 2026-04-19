package me.jianwen.mediask.domain.clinical.model;

public record EmrDiagnosis(
        DiagnosisType diagnosisType,
        String diagnosisCode,
        String diagnosisName,
        boolean isPrimary,
        int sortOrder) {

    public enum DiagnosisType {
        PRIMARY,
        SECONDARY
    }

    public EmrDiagnosis {
        if (diagnosisType == DiagnosisType.PRIMARY != isPrimary) {
            throw new IllegalArgumentException("diagnosisType and isPrimary are inconsistent");
        }
        if (diagnosisName == null || diagnosisName.isBlank()) {
            throw new IllegalArgumentException("diagnosisName cannot be null or blank");
        }
        if (sortOrder < 0) {
            throw new IllegalArgumentException("sortOrder cannot be negative");
        }
    }

    public static EmrDiagnosis createPrimary(String diagnosisCode, String diagnosisName, int sortOrder) {
        return new EmrDiagnosis(DiagnosisType.PRIMARY, diagnosisCode, diagnosisName, true, sortOrder);
    }

    public static EmrDiagnosis createSecondary(String diagnosisCode, String diagnosisName, int sortOrder) {
        return new EmrDiagnosis(DiagnosisType.SECONDARY, diagnosisCode, diagnosisName, false, sortOrder);
    }
}