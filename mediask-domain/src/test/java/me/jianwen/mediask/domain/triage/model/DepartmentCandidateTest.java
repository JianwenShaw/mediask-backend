package me.jianwen.mediask.domain.triage.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class DepartmentCandidateTest {

    @Test
    void constructor_ValidInputs_CreatesInstance() {
        DepartmentCandidate candidate = new DepartmentCandidate(
                101L, "神经内科", "头痛头晕相关", List.of("神内"), 10);
        assertEquals(101L, candidate.departmentId());
        assertEquals("神经内科", candidate.departmentName());
        assertEquals("头痛头晕相关", candidate.routingHint());
        assertEquals(List.of("神内"), candidate.aliases());
        assertEquals(10, candidate.sortOrder());
    }

    @Test
    void constructor_NullAliases_DefaultsToEmptyList() {
        DepartmentCandidate candidate = new DepartmentCandidate(
                101L, "神经内科", "头痛头晕相关", null, 10);
        assertNotNull(candidate.aliases());
        assertTrue(candidate.aliases().isEmpty());
    }

    @Test
    void constructor_BlankDepartmentName_Throws() {
        assertThrows(IllegalArgumentException.class, () ->
                new DepartmentCandidate(101L, "", "头痛头晕相关", List.of(), 10));
    }

    @Test
    void constructor_ZeroDepartmentId_Throws() {
        assertThrows(IllegalArgumentException.class, () ->
                new DepartmentCandidate(0L, "神经内科", "头痛头晕相关", List.of(), 10));
    }

    @Test
    void constructor_NegativeSortOrder_Throws() {
        assertThrows(IllegalArgumentException.class, () ->
                new DepartmentCandidate(101L, "神经内科", "头痛头晕相关", List.of(), -1));
    }

    @Test
    void constructor_AliasesAreDefensivelyCopied() {
        List<String> mutable = new java.util.ArrayList<>();
        mutable.add("神内");
        DepartmentCandidate candidate = new DepartmentCandidate(
                101L, "神经内科", "头痛头晕相关", mutable, 10);
        mutable.add("脑病门诊");
        assertEquals(1, candidate.aliases().size());
    }
}
