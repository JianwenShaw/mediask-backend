package me.jianwen.mediask.domain.triage.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.OffsetDateTime;
import java.util.List;
import me.jianwen.mediask.common.exception.BizException;
import me.jianwen.mediask.domain.triage.exception.TriageErrorCode;
import me.jianwen.mediask.domain.triage.model.CatalogVersion;
import me.jianwen.mediask.domain.triage.model.DepartmentCandidate;
import me.jianwen.mediask.domain.triage.model.TriageCatalog;
import org.junit.jupiter.api.Test;

class TriageCatalogValidatorTest {

    private static final CatalogVersion VERSION = new CatalogVersion("deptcat-v20260428-01");

    private static TriageCatalog catalog() {
        return new TriageCatalog("default", VERSION, OffsetDateTime.now(), List.of(
                new DepartmentCandidate(101L, "神经内科", "头痛头晕", List.of("神内"), 10),
                new DepartmentCandidate(102L, "心内科", "胸闷胸痛", List.of(), 20)));
    }

    @Test
    void validate_ValidDepartmentAndName_DoesNotThrow() {
        assertDoesNotThrow(() -> TriageCatalogValidator.validate(catalog(), 101L, "神经内科"));
    }

    @Test
    void validate_DepartmentNotInCatalog_Throws() {
        BizException ex = assertThrows(BizException.class,
                () -> TriageCatalogValidator.validate(catalog(), 999L, "不存在"));
        assertEquals(TriageErrorCode.DEPARTMENT_NOT_IN_CATALOG, ex.getErrorCode());
    }

    @Test
    void validate_NameMismatch_Throws() {
        BizException ex = assertThrows(BizException.class,
                () -> TriageCatalogValidator.validate(catalog(), 101L, "心内科"));
        assertEquals(TriageErrorCode.DEPARTMENT_NAME_MISMATCH, ex.getErrorCode());
    }

    @Test
    void validate_NameWithTrailingSpace_Mismatch() {
        BizException ex = assertThrows(BizException.class,
                () -> TriageCatalogValidator.validate(catalog(), 101L, "神经内科 "));
        assertEquals(TriageErrorCode.DEPARTMENT_NAME_MISMATCH, ex.getErrorCode());
    }
}
