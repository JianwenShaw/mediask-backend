package me.jianwen.mediask.infra.persistence.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import me.jianwen.mediask.common.exception.ErrorCode;
import me.jianwen.mediask.common.exception.SysException;
import me.jianwen.mediask.domain.user.model.DoctorProfile;
import me.jianwen.mediask.infra.cache.RedisJsonCacheHelper;
import me.jianwen.mediask.infra.persistence.dataobject.DepartmentDO;
import me.jianwen.mediask.infra.persistence.dataobject.DoctorDO;
import me.jianwen.mediask.infra.persistence.mapper.DepartmentMapper;
import me.jianwen.mediask.infra.persistence.mapper.DoctorDepartmentRelationMapper;
import me.jianwen.mediask.infra.persistence.mapper.DoctorMapper;
import org.junit.jupiter.api.Test;

class DoctorProfileRepositoryAdapterTest {

    @Test
    void findByUserId_WhenCacheHit_ReturnCachedProfile() {
        StubRedisJsonCacheHelper cacheHelper = new StubRedisJsonCacheHelper();
        cacheHelper.cachedDoctorProfile = Optional.of(
                new DoctorProfile(1001L, "DOC-1001", "Chief Physician", "Experienced", 12L, 7L, "Cardiology"));
        AtomicInteger doctorQueryCount = new AtomicInteger();
        DoctorProfileRepositoryAdapter adapter = new DoctorProfileRepositoryAdapter(
                cacheHelper,
                proxy(DoctorMapper.class, Map.of("selectOne", arguments -> {
                    doctorQueryCount.incrementAndGet();
                    return null;
                })),
                proxy(DoctorDepartmentRelationMapper.class, Map.of("selectPrimaryDepartmentIdByDoctorId", arguments -> null)),
                proxy(DepartmentMapper.class, Map.of("selectOne", arguments -> null)));

        Optional<DoctorProfile> doctorProfile = adapter.findByUserId(1001L);

        assertTrue(doctorProfile.isPresent());
        assertEquals("DOC-1001", doctorProfile.orElseThrow().doctorCode());
        assertEquals(0, doctorQueryCount.get());
    }

    @Test
    void findByUserId_WhenCacheMiss_QueryDatabaseAndBackfillCache() {
        StubRedisJsonCacheHelper cacheHelper = new StubRedisJsonCacheHelper();
        DoctorDO doctorDO = new DoctorDO();
        doctorDO.setId(1001L);
        doctorDO.setUserId(2001L);
        doctorDO.setDoctorCode("DOC-1001");
        doctorDO.setProfessionalTitle("Chief Physician");
        doctorDO.setIntroductionMasked("Experienced");
        doctorDO.setHospitalId(12L);
        DepartmentDO departmentDO = new DepartmentDO();
        departmentDO.setId(7L);
        departmentDO.setName("Cardiology");
        DoctorProfileRepositoryAdapter adapter = new DoctorProfileRepositoryAdapter(
                cacheHelper,
                proxy(DoctorMapper.class, Map.of("selectOne", arguments -> doctorDO)),
                proxy(DoctorDepartmentRelationMapper.class, Map.of("selectPrimaryDepartmentIdByDoctorId", arguments -> 7L)),
                proxy(DepartmentMapper.class, Map.of("selectOne", arguments -> departmentDO)));

        DoctorProfile doctorProfile = adapter.findByUserId(2001L).orElseThrow();

        assertEquals("DOC-1001", doctorProfile.doctorCode());
        assertEquals("user:doctor-profile:2001", cacheHelper.lastPutKey);
        assertEquals("DOC-1001", cacheHelper.lastDoctorProfile.doctorCode());
    }

    @Test
    void findByUserId_WhenDatabaseMiss_ReturnEmpty() {
        StubRedisJsonCacheHelper cacheHelper = new StubRedisJsonCacheHelper();
        DoctorProfileRepositoryAdapter adapter = new DoctorProfileRepositoryAdapter(
                cacheHelper,
                proxy(DoctorMapper.class, Map.of("selectOne", arguments -> null)),
                proxy(DoctorDepartmentRelationMapper.class, Map.of("selectPrimaryDepartmentIdByDoctorId", arguments -> null)),
                proxy(DepartmentMapper.class, Map.of("selectOne", arguments -> null)));

        assertTrue(adapter.findByUserId(2001L).isEmpty());
    }

    @Test
    void findByUserId_WhenCacheReadFails_ThrowSysException() {
        StubRedisJsonCacheHelper cacheHelper = new StubRedisJsonCacheHelper();
        cacheHelper.readException = new SysException(ErrorCode.SYSTEM_ERROR, "cache read failed");
        DoctorProfileRepositoryAdapter adapter = new DoctorProfileRepositoryAdapter(
                cacheHelper,
                proxy(DoctorMapper.class, Map.of("selectOne", arguments -> null)),
                proxy(DoctorDepartmentRelationMapper.class, Map.of("selectPrimaryDepartmentIdByDoctorId", arguments -> null)),
                proxy(DepartmentMapper.class, Map.of("selectOne", arguments -> null)));

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

        private Optional<DoctorProfile> cachedDoctorProfile = Optional.empty();
        private SysException readException;
        private String lastPutKey;
        private DoctorProfile lastDoctorProfile;

        private StubRedisJsonCacheHelper() {
            super(null, null);
        }

        @Override
        public <T> Optional<T> get(String key, Class<T> valueType) {
            if (readException != null) {
                throw readException;
            }
            if (valueType == DoctorProfile.class) {
                return cachedDoctorProfile.map(valueType::cast);
            }
            return Optional.empty();
        }

        @Override
        public void put(String key, Object value, java.time.Duration ttl) {
            this.lastPutKey = key;
            this.lastDoctorProfile = (DoctorProfile) value;
        }
    }
}
