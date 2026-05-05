package me.jianwen.mediask.domain.clinical.model;

import java.time.Instant;
import java.util.List;
import me.jianwen.mediask.common.id.SnowflakeIdGenerator;

public record PrescriptionOrder(
        Long prescriptionOrderId,
        String prescriptionNo,
        Long recordId,
        Long encounterId,
        Long patientId,
        Long doctorId,
        PrescriptionStatus prescriptionStatus,
        List<PrescriptionItem> items,
        int version,
        Instant createdAt,
        Instant updatedAt) {

    public PrescriptionOrder {
        if (prescriptionOrderId == null) {
            throw new IllegalArgumentException("prescriptionOrderId cannot be null");
        }
        if (prescriptionNo == null || prescriptionNo.isBlank()) {
            throw new IllegalArgumentException("prescriptionNo cannot be null or blank");
        }
        if (recordId == null) {
            throw new IllegalArgumentException("recordId cannot be null");
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
        if (prescriptionStatus == null) {
            throw new IllegalArgumentException("prescriptionStatus cannot be null");
        }
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("items cannot be null or empty");
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

    public static PrescriptionOrder createDraft(
            String prescriptionNo,
            Long recordId,
            Long encounterId,
            Long patientId,
            Long doctorId,
            List<PrescriptionItem> items) {
        Instant now = Instant.now();
        return new PrescriptionOrder(
                SnowflakeIdGenerator.nextId(),
                prescriptionNo,
                recordId,
                encounterId,
                patientId,
                doctorId,
                PrescriptionStatus.DRAFT,
                List.copyOf(items),
                0,
                now,
                now);
    }

    public PrescriptionOrder updateItems(List<PrescriptionItem> newItems) {
        if (prescriptionStatus != PrescriptionStatus.DRAFT) {
            throw new IllegalStateException("Only DRAFT prescriptions can have items updated");
        }
        if (newItems == null || newItems.isEmpty()) {
            throw new IllegalArgumentException("items cannot be null or empty");
        }
        Instant now = Instant.now();
        return new PrescriptionOrder(
                prescriptionOrderId, prescriptionNo, recordId, encounterId,
                patientId, doctorId, prescriptionStatus,
                List.copyOf(newItems),
                version, createdAt, now);
    }

    public PrescriptionOrder issue() {
        if (prescriptionStatus != PrescriptionStatus.DRAFT) {
            throw new IllegalStateException("Only DRAFT prescriptions can be issued");
        }
        Instant now = Instant.now();
        return new PrescriptionOrder(
                prescriptionOrderId, prescriptionNo, recordId, encounterId,
                patientId, doctorId, PrescriptionStatus.ISSUED,
                items,
                version, createdAt, now);
    }

    public PrescriptionOrder cancel() {
        if (prescriptionStatus != PrescriptionStatus.DRAFT
                && prescriptionStatus != PrescriptionStatus.ISSUED) {
            throw new IllegalStateException("Only DRAFT or ISSUED prescriptions can be cancelled");
        }
        Instant now = Instant.now();
        return new PrescriptionOrder(
                prescriptionOrderId, prescriptionNo, recordId, encounterId,
                patientId, doctorId, PrescriptionStatus.CANCELLED,
                items,
                version, createdAt, now);
    }
}
