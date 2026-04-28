package me.jianwen.mediask.domain.triage.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class TriageCatalogTest {

    private static final CatalogVersion VERSION = new CatalogVersion("deptcat-v20260428-01");
    private static final OffsetDateTime NOW = OffsetDateTime.now();

    @Test
    void constructor_ValidInputs_CreatesInstance() {
        List<DepartmentCandidate> candidates = List.of(
                new DepartmentCandidate(101L, "神经内科", "头痛头晕", List.of("神内"), 10));
        TriageCatalog catalog = new TriageCatalog("default", VERSION, NOW, candidates);
        assertEquals("default", catalog.hospitalScope());
        assertEquals(VERSION, catalog.catalogVersion());
        assertEquals(1, catalog.candidateCount());
    }

    @Test
    void constructor_EmptyCandidates_Throws() {
        assertThrows(IllegalArgumentException.class, () ->
                new TriageCatalog("default", VERSION, NOW, List.of()));
    }

    @Test
    void findCandidate_ExistingId_ReturnsCandidate() {
        List<DepartmentCandidate> candidates = List.of(
                new DepartmentCandidate(101L, "神经内科", "头痛头晕", List.of(), 10),
                new DepartmentCandidate(102L, "心内科", "胸闷胸痛", List.of(), 20));
        TriageCatalog catalog = new TriageCatalog("default", VERSION, NOW, candidates);

        Optional<DepartmentCandidate> result = catalog.findCandidate(101L);
        assertTrue(result.isPresent());
        assertEquals("神经内科", result.get().departmentName());
    }

    @Test
    void findCandidate_NonExistingId_ReturnsEmpty() {
        List<DepartmentCandidate> candidates = List.of(
                new DepartmentCandidate(101L, "神经内科", "头痛头晕", List.of(), 10));
        TriageCatalog catalog = new TriageCatalog("default", VERSION, NOW, candidates);

        assertFalse(catalog.findCandidate(999L).isPresent());
    }

    @Test
    void containsDepartment_ExistingId_ReturnsTrue() {
        List<DepartmentCandidate> candidates = List.of(
                new DepartmentCandidate(101L, "神经内科", "头痛头晕", List.of(), 10));
        TriageCatalog catalog = new TriageCatalog("default", VERSION, NOW, candidates);

        assertTrue(catalog.containsDepartment(101L));
    }

    @Test
    void containsDepartment_NonExistingId_ReturnsFalse() {
        List<DepartmentCandidate> candidates = List.of(
                new DepartmentCandidate(101L, "神经内科", "头痛头晕", List.of(), 10));
        TriageCatalog catalog = new TriageCatalog("default", VERSION, NOW, candidates);

        assertFalse(catalog.containsDepartment(999L));
    }
}
