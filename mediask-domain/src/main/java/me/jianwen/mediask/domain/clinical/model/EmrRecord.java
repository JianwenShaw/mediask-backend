package me.jianwen.mediask.domain.clinical.model;

import me.jianwen.mediask.common.id.SnowflakeIdGenerator;

import java.time.Instant;
import java.util.List;

public record EmrRecord(
        Long recordId,
        String recordNo,
        Long encounterId,
        Long patientId,
        Long doctorId,
        Long departmentId,
        EmrRecordStatus recordStatus,
        String chiefComplaintSummary,
        String content,
        List<EmrDiagnosis> diagnoses,
        int version,
        Instant createdAt,
        Instant updatedAt) {

    public EmrRecord {
        if (recordId == null) {
            throw new IllegalArgumentException("recordId cannot be null");
        }
        if (recordNo == null || recordNo.isBlank()) {
            throw new IllegalArgumentException("recordNo cannot be null or blank");
        }
        if (encounterId == null) {
            throw new IllegalArgumentException("encounterId cannot be null");
        }
        if (patientId == null) {
            throw new IllegalArgumentException("patientId cannot be null");
        }
        if (doctorId == null) {
            throw new IllegalArgumentException("doctorId cannot be null");
        }
        if (departmentId == null) {
            throw new IllegalArgumentException("departmentId cannot be null");
        }
        if (recordStatus == null) {
            throw new IllegalArgumentException("recordStatus cannot be null");
        }
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("content cannot be null or blank");
        }
        if (diagnoses == null) {
            throw new IllegalArgumentException("diagnoses cannot be null");
        }
        if (version < 0) {
            throw new IllegalArgumentException("version cannot be negative");
        }
        if (createdAt == null) {
            throw new IllegalArgumentException("createdAt cannot be null");
        }
        if (updatedAt == null) {
            throw new IllegalArgumentException("updatedAt cannot be null");
        }
    }

    public static EmrRecord createDraft(
            String recordNo,
            Long encounterId,
            Long patientId,
            Long doctorId,
            Long departmentId,
            String chiefComplaintSummary,
            String content,
            List<EmrDiagnosis> diagnoses) {
        Instant now = Instant.now();
        return new EmrRecord(
                SnowflakeIdGenerator.nextId(),
                recordNo,
                encounterId,
                patientId,
                doctorId,
                departmentId,
                EmrRecordStatus.DRAFT,
                chiefComplaintSummary,
                content,
                diagnoses,
                0,
                now,
                now);
    }

    public EmrRecord sign() {
        if (recordStatus != EmrRecordStatus.DRAFT) {
            throw new IllegalStateException("Only DRAFT records can be signed");
        }
        Instant now = Instant.now();
        return new EmrRecord(
                recordId,
                recordNo,
                encounterId,
                patientId,
                doctorId,
                departmentId,
                EmrRecordStatus.SIGNED,
                chiefComplaintSummary,
                content,
                diagnoses,
                version + 1,
                createdAt,
                now);
    }

    public EmrRecord amend(String newContent, List<EmrDiagnosis> newDiagnoses) {
        if (recordStatus != EmrRecordStatus.SIGNED) {
            throw new IllegalStateException("Only SIGNED records can be amended");
        }
        if (newContent == null || newContent.isBlank()) {
            throw new IllegalArgumentException("newContent cannot be null or blank");
        }
        if (newDiagnoses == null) {
            throw new IllegalArgumentException("newDiagnoses cannot be null");
        }
        Instant now = Instant.now();
        return new EmrRecord(
                recordId,
                recordNo,
                encounterId,
                patientId,
                doctorId,
                departmentId,
                EmrRecordStatus.AMENDED,
                chiefComplaintSummary,
                newContent,
                newDiagnoses,
                version + 1,
                createdAt,
                now);
    }
}