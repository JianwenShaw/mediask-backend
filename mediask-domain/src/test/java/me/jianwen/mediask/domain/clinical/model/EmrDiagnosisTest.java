package me.jianwen.mediask.domain.clinical.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EmrDiagnosisTest {

    @Test
    void createPrimary_ValidInput_Success() {
        EmrDiagnosis diagnosis = EmrDiagnosis.createPrimary("J01.90", "Acute sinusitis", 0);

        assertEquals(EmrDiagnosis.DiagnosisType.PRIMARY, diagnosis.diagnosisType());
        assertEquals("J01.90", diagnosis.diagnosisCode());
        assertEquals("Acute sinusitis", diagnosis.diagnosisName());
        assertTrue(diagnosis.isPrimary());
        assertEquals(0, diagnosis.sortOrder());
    }

    @Test
    void createSecondary_ValidInput_Success() {
        EmrDiagnosis diagnosis = EmrDiagnosis.createSecondary("J06.9", "Acute upper respiratory infection", 1);

        assertEquals(EmrDiagnosis.DiagnosisType.SECONDARY, diagnosis.diagnosisType());
        assertEquals("J06.9", diagnosis.diagnosisCode());
        assertEquals("Acute upper respiratory infection", diagnosis.diagnosisName());
        assertFalse(diagnosis.isPrimary());
        assertEquals(1, diagnosis.sortOrder());
    }

    @Test
    void createPrimary_NullDiagnosisName_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> EmrDiagnosis.createPrimary("J01.90", null, 0));
    }

    @Test
    void createPrimary_BlankDiagnosisName_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> EmrDiagnosis.createPrimary("J01.90", "   ", 0));
    }

    @Test
    void createSecondary_NegativeSortOrder_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> EmrDiagnosis.createSecondary("J06.9", "Diagnosis", -1));
    }

    @Test
    void createPrimary_NullDiagnosisCode_Success() {
        EmrDiagnosis diagnosis = EmrDiagnosis.createPrimary(null, "Acute sinusitis", 0);

        assertNull(diagnosis.diagnosisCode());
        assertEquals("Acute sinusitis", diagnosis.diagnosisName());
    }
}
