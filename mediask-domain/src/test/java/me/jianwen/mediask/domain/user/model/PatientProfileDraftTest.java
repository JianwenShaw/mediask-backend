package me.jianwen.mediask.domain.user.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class PatientProfileDraftTest {

    @Test
    void constructor_WhenInputContainsBlanks_NormalizeFields() {
        PatientProfileDraft draft = new PatientProfileDraft("  FEMALE  ", null, "  A  ", "   ");

        assertEquals("FEMALE", draft.gender());
        assertEquals("A", draft.bloodType());
        assertNull(draft.allergySummary());
    }

    @Test
    void constructor_WhenGenderHasMixedCase_NormalizeToUpperCase() {
        PatientProfileDraft draft = new PatientProfileDraft("  female  ", null, null, null);

        assertEquals("FEMALE", draft.gender());
    }

    @Test
    void constructor_WhenGenderIsInvalid_ThrowIllegalArgumentException() {
        IllegalArgumentException exception =
                assertThrows(IllegalArgumentException.class, () -> new PatientProfileDraft("UNKNOWN", null, null, null));

        assertEquals("gender must be one of MALE, FEMALE, OTHER", exception.getMessage());
    }
}
