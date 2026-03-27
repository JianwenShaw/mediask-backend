package me.jianwen.mediask.infra.persistence.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Proxy;
import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import me.jianwen.mediask.common.exception.ErrorCode;
import me.jianwen.mediask.common.exception.SysException;
import me.jianwen.mediask.domain.user.model.PatientProfileSnapshot;
import me.jianwen.mediask.infra.cache.RedisJsonCacheHelper;
import me.jianwen.mediask.infra.persistence.dataobject.PatientProfileDO;
import me.jianwen.mediask.infra.persistence.mapper.PatientProfileMapper;
import org.junit.jupiter.api.Test;

class PatientProfileRepositoryAdapterTest {

    @Test
    void findByUserId_WhenCacheHit_ReturnCachedProfile() {
        StubRedisJsonCacheHelper cacheHelper = new StubRedisJsonCacheHelper();
        cacheHelper.cachedPatientProfile = Optional.of(
                new PatientProfileSnapshot(3001L, "PAT-3001", "FEMALE", LocalDate.of(1992, 5, 1), "A", "Peanut"));
        PatientProfileRepositoryAdapter adapter =
                new PatientProfileRepositoryAdapter(cacheHelper, proxy(PatientProfileMapper.class, Map.of("selectOne", arguments -> null)));

        Optional<PatientProfileSnapshot> patientProfile = adapter.findByUserId(2001L);

        assertTrue(patientProfile.isPresent());
        assertEquals("PAT-3001", patientProfile.orElseThrow().patientNo());
    }

    @Test
    void findByUserId_WhenCacheMiss_QueryDatabaseAndBackfillCache() {
        StubRedisJsonCacheHelper cacheHelper = new StubRedisJsonCacheHelper();
        PatientProfileDO patientProfileDO = new PatientProfileDO();
        patientProfileDO.setId(3001L);
        patientProfileDO.setUserId(2001L);
        patientProfileDO.setPatientNo("PAT-3001");
        patientProfileDO.setGender("FEMALE");
        patientProfileDO.setBirthDate(LocalDate.of(1992, 5, 1));
        patientProfileDO.setBloodType("A");
        patientProfileDO.setAllergySummary("Peanut");
        PatientProfileRepositoryAdapter adapter =
                new PatientProfileRepositoryAdapter(cacheHelper, proxy(PatientProfileMapper.class, Map.of("selectOne", arguments -> patientProfileDO)));

        PatientProfileSnapshot patientProfile = adapter.findByUserId(2001L).orElseThrow();

        assertEquals("PAT-3001", patientProfile.patientNo());
        assertEquals("user:patient-profile:2001", cacheHelper.lastPutKey);
        assertEquals("PAT-3001", cacheHelper.lastPatientProfile.patientNo());
    }

    @Test
    void findByUserId_WhenDatabaseMiss_ReturnEmpty() {
        StubRedisJsonCacheHelper cacheHelper = new StubRedisJsonCacheHelper();
        PatientProfileRepositoryAdapter adapter =
                new PatientProfileRepositoryAdapter(cacheHelper, proxy(PatientProfileMapper.class, Map.of("selectOne", arguments -> null)));

        assertTrue(adapter.findByUserId(2001L).isEmpty());
    }

    @Test
    void findByUserId_WhenCacheReadFails_ThrowSysException() {
        StubRedisJsonCacheHelper cacheHelper = new StubRedisJsonCacheHelper();
        cacheHelper.readException = new SysException(ErrorCode.SYSTEM_ERROR, "cache read failed");
        PatientProfileRepositoryAdapter adapter =
                new PatientProfileRepositoryAdapter(cacheHelper, proxy(PatientProfileMapper.class, Map.of("selectOne", arguments -> null)));

        assertThrows(SysException.class, () -> adapter.findByUserId(2001L));
    }

    private static <T> T proxy(Class<T> type, Map<String, Function<Object[], Object>> handlers) {
        Object proxyInstance = Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] {type}, (proxy, method, args) -> {
            if (method.getDeclaringClass() == Object.class) {
                return switch (method.getName()) {
                    case "equals" -> proxy == args[0];
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "toString" -> type.getSimpleName() + "TestProxy";
                    default -> throw new UnsupportedOperationException(method.getName());
                };
            }
            Function<Object[], Object> handler = handlers.get(method.getName());
            if (handler == null) {
                throw new AssertionError("No test handler registered for " + type.getSimpleName() + "#" + method.getName());
            }
            return handler.apply(args == null ? new Object[0] : args);
        });
        return type.cast(proxyInstance);
    }

    private static final class StubRedisJsonCacheHelper extends RedisJsonCacheHelper {

        private Optional<PatientProfileSnapshot> cachedPatientProfile = Optional.empty();
        private SysException readException;
        private String lastPutKey;
        private PatientProfileSnapshot lastPatientProfile;

        private StubRedisJsonCacheHelper() {
            super(null, null);
        }

        @Override
        public <T> Optional<T> get(String key, Class<T> valueType) {
            if (readException != null) {
                throw readException;
            }
            if (valueType == PatientProfileSnapshot.class) {
                return cachedPatientProfile.map(valueType::cast);
            }
            return Optional.empty();
        }

        @Override
        public void put(String key, Object value, java.time.Duration ttl) {
            this.lastPutKey = key;
            this.lastPatientProfile = (PatientProfileSnapshot) value;
        }
    }
}
