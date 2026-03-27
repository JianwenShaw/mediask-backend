package me.jianwen.mediask.domain.user.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class DoctorProfileDraftTest {

    @Test
    void constructor_WhenInputContainsBlanks_NormalizeFields() {
        DoctorProfileDraft draft = new DoctorProfileDraft("  Chief Physician  ", "   ");

        assertEquals("Chief Physician", draft.professionalTitle());
        assertNull(draft.introductionMasked());
    }
}
