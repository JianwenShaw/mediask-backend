package me.jianwen.mediask.application.user.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class CreateAdminPatientCommandTest {

    @Test
    void constructor_WhenPasswordHasEdgeSpaces_PreserveRawPassword() {
        CreateAdminPatientCommand command = new CreateAdminPatientCommand(
                "patient_new", "13700009999", "  patient123  ", "李新患者", "137****1234", "FEMALE", LocalDate.of(1995, 6, 1), "A", "Peanut");

        assertEquals("  patient123  ", command.password());
    }

    @Test
    void constructor_WhenPasswordIsBlank_ThrowIllegalArgumentException() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new CreateAdminPatientCommand(
                        "patient_new", "13700009999", "   ", "李新患者", "137****1234", "FEMALE", LocalDate.of(1995, 6, 1), "A", "Peanut"));
    }
}
