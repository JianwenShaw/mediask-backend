package me.jianwen.mediask.domain.clinical.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class EmrRecordTest {

    @Test
    void createDraft_ValidInput_Success() {
        List<EmrDiagnosis> diagnoses = List.of(
                EmrDiagnosis.createPrimary("J01.90", "Acute sinusitis", 0)
        );

        EmrRecord record = EmrRecord.createDraft(
                "EMR123",
                1L,
                100L,
                200L,
                300L,
                "Patient has headache and congestion",
                "Detailed medical history and examination findings...",
                diagnoses
        );

        assertNotNull(record.recordId());
        assertEquals("EMR123", record.recordNo());
        assertEquals(1L, record.encounterId());
        assertEquals(100L, record.patientId());
        assertEquals(200L, record.doctorId());
        assertEquals(300L, record.departmentId());
        assertEquals(EmrRecordStatus.DRAFT, record.recordStatus());
        assertEquals("Patient has headache and congestion", record.chiefComplaintSummary());
        assertEquals("Detailed medical history and examination findings...", record.content());
        assertEquals(1, record.diagnoses().size());
        assertEquals(0, record.version());
        assertNotNull(record.createdAt());
        assertNotNull(record.updatedAt());
    }

    @Test
    void createDraft_NullContent_ThrowsException() {
        List<EmrDiagnosis> diagnoses = List.of();

        assertThrows(IllegalArgumentException.class, () -> EmrRecord.createDraft(
                "EMR123", 1L, 100L, 200L, 300L, "Summary", null, diagnoses
        ));
    }

    @Test
    void createDraft_BlankContent_ThrowsException() {
        List<EmrDiagnosis> diagnoses = List.of();

        assertThrows(IllegalArgumentException.class, () -> EmrRecord.createDraft(
                "EMR123", 1L, 100L, 200L, 300L, "Summary", "   ", diagnoses
        ));
    }

    @Test
    void sign_DraftRecord_SignedRecord() {
        List<EmrDiagnosis> diagnoses = List.of(
                EmrDiagnosis.createPrimary("J01.90", "Acute sinusitis", 0)
        );

        EmrRecord draft = EmrRecord.createDraft(
                "EMR123", 1L, 100L, 200L, 300L, "Summary", "Content...", diagnoses
        );

        EmrRecord signed = draft.sign();

        assertEquals(EmrRecordStatus.SIGNED, signed.recordStatus());
        assertEquals(1, signed.version());
        assertTrue(signed.updatedAt().isAfter(draft.updatedAt()));
        assertEquals(draft.recordId(), signed.recordId());
    }

    @Test
    void sign_SignedRecord_ThrowsException() {
        List<EmrDiagnosis> diagnoses = List.of();

        EmrRecord draft = EmrRecord.createDraft(
                "EMR123", 1L, 100L, 200L, 300L, "Summary", "Content...", diagnoses
        );

        EmrRecord signed = draft.sign();

        assertThrows(IllegalStateException.class, signed::sign);
    }

    @Test
    void amend_SignedRecord_AmendedRecord() {
        List<EmrDiagnosis> originalDiagnoses = List.of(
                EmrDiagnosis.createPrimary("J01.90", "Acute sinusitis", 0)
        );

        EmrRecord signed = EmrRecord.createDraft(
                "EMR123", 1L, 100L, 200L, 300L, "Summary", "Original content...", originalDiagnoses
        ).sign();

        List<EmrDiagnosis> newDiagnoses = List.of(
                EmrDiagnosis.createPrimary("J01.91", "Acute recurrent sinusitis", 0)
        );

        EmrRecord amended = signed.amend("Updated content with new findings...", newDiagnoses);

        assertEquals(EmrRecordStatus.AMENDED, amended.recordStatus());
        assertEquals(2, amended.version());
        assertEquals("Updated content with new findings...", amended.content());
        assertEquals(1, amended.diagnoses().size());
        assertEquals("Acute recurrent sinusitis", amended.diagnoses().get(0).diagnosisName());
    }

    @Test
    void amend_DraftRecord_ThrowsException() {
        List<EmrDiagnosis> diagnoses = List.of();

        EmrRecord draft = EmrRecord.createDraft(
                "EMR123", 1L, 100L, 200L, 300L, "Summary", "Content...", diagnoses
        );

        assertThrows(IllegalStateException.class, () -> draft.amend("New content", diagnoses));
    }
}
