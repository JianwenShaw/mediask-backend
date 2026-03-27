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
}
