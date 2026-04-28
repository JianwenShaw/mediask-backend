package me.jianwen.mediask.infra.cache;

import static org.junit.jupiter.api.Assertions.assertEquals;

import me.jianwen.mediask.common.cache.CacheKeyGenerator;
import org.junit.jupiter.api.Test;

class CacheKeyGeneratorTest {

    @Test
    void doctorProfileByUserId_ReturnExpectedKey() {
        assertEquals("user:doctor-profile:1001", CacheKeyGenerator.doctorProfileByUserId(1001L));
    }

    @Test
    void patientProfileByUserId_ReturnExpectedKey() {
        assertEquals("user:patient-profile:1001", CacheKeyGenerator.patientProfileByUserId(1001L));
    }

    @Test
    void triageCatalogActiveVersion_ReturnExpectedKey() {
        assertEquals("triage_catalog:active:default",
                CacheKeyGenerator.triageCatalogActiveVersion("default"));
    }

    @Test
    void triageCatalogContent_ReturnExpectedKey() {
        assertEquals("triage_catalog:default:deptcat-v20260423-01",
                CacheKeyGenerator.triageCatalogContent("default", "deptcat-v20260423-01"));
    }

    @Test
    void triageCatalogSequenceCounter_ReturnExpectedKey() {
        assertEquals("triage_catalog:seq:default:20260423",
                CacheKeyGenerator.triageCatalogSequenceCounter("default", "20260423"));
    }
}
