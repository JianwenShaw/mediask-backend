package me.jianwen.mediask.api.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class CreateAdminPatientRequestTest {

    @Test
    void constructor_WhenPasswordHasEdgeSpaces_PreserveRawPassword() {
        CreateAdminPatientRequest request = new CreateAdminPatientRequest(
                "patient_new", "  patient123  ", "李新患者", "137****1234", "FEMALE", LocalDate.of(1995, 6, 1), "A", "Peanut");

        assertEquals("  patient123  ", request.password());
    }

    @Test
    void constructor_WhenPasswordIsBlank_ThrowIllegalArgumentException() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new CreateAdminPatientRequest(
                        "patient_new", "   ", "李新患者", "137****1234", "FEMALE", LocalDate.of(1995, 6, 1), "A", "Peanut"));
    }
}
