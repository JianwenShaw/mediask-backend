package me.jianwen.mediask.domain.triage.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class CatalogVersionTest {

    @Test
    void constructor_ValidFormat_ParsesCorrectly() {
        CatalogVersion version = new CatalogVersion("deptcat-v20260428-01");
        assertEquals("deptcat-v20260428-01", version.value());
        assertEquals(LocalDate.of(2026, 4, 28), version.date());
        assertEquals(1, version.sequence());
    }

    @Test
    void constructor_InvalidFormat_Throws() {
        assertThrows(IllegalArgumentException.class, () -> new CatalogVersion(""));
        assertThrows(IllegalArgumentException.class, () -> new CatalogVersion("invalid"));
        assertThrows(IllegalArgumentException.class, () -> new CatalogVersion("deptcat-v2026042-01"));
        assertThrows(IllegalArgumentException.class, () -> new CatalogVersion("deptcat-v20260428-1"));
        assertThrows(IllegalArgumentException.class, () -> new CatalogVersion("deptcat-20260428-01"));
    }

    @Test
    void constructor_ValidString_CreatesInstance() {
        CatalogVersion version = new CatalogVersion("deptcat-v20260428-05");
        assertEquals(5, version.sequence());
    }

    @Test
    void of_ValidInputs_ConstructsCorrectString() {
        CatalogVersion version = CatalogVersion.of(LocalDate.of(2026, 4, 28), 3);
        assertEquals("deptcat-v20260428-03", version.value());
    }

    @Test
    void of_SequenceOutOfRange_Throws() {
        LocalDate date = LocalDate.of(2026, 4, 28);
        assertThrows(IllegalArgumentException.class, () -> CatalogVersion.of(date, 0));
        assertThrows(IllegalArgumentException.class, () -> CatalogVersion.of(date, 100));
    }

    @Test
    void isSameDay_MatchingDate_ReturnsTrue() {
        CatalogVersion version = new CatalogVersion("deptcat-v20260428-01");
        assertTrue(version.isSameDay(LocalDate.of(2026, 4, 28)));
    }

    @Test
    void isSameDay_NonMatchingDate_ReturnsFalse() {
        CatalogVersion version = new CatalogVersion("deptcat-v20260428-01");
        assertFalse(version.isSameDay(LocalDate.of(2026, 4, 29)));
    }

    @Test
    void value_JsonValue_ReturnsFlatString() {
        CatalogVersion version = new CatalogVersion("deptcat-v20260428-01");
        assertEquals("deptcat-v20260428-01", version.value());
    }
}
