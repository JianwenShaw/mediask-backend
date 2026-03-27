package me.jianwen.mediask.infra.persistence.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.function.Function;
import me.jianwen.mediask.common.exception.BizException;
import me.jianwen.mediask.common.exception.ErrorCode;
import me.jianwen.mediask.common.exception.SysException;
import me.jianwen.mediask.domain.user.exception.UserErrorCode;
import me.jianwen.mediask.domain.user.model.DoctorProfileDraft;
import me.jianwen.mediask.infra.cache.RedisJsonCacheHelper;
import me.jianwen.mediask.infra.persistence.dataobject.DoctorDO;
import me.jianwen.mediask.infra.persistence.mapper.DoctorMapper;
import org.junit.jupiter.api.Test;

class DoctorProfileWriteRepositoryAdapterTest {

    @Test
    void updateByUserId_WhenUpdateSucceeds_DeleteCache() {
        StubRedisJsonCacheHelper cacheHelper = new StubRedisJsonCacheHelper();
        DoctorDO existingDoctor = createDoctorDO();
        CapturedValue<DoctorDO> updatedDoctor = new CapturedValue<>();
        DoctorProfileWriteRepositoryAdapter adapter = new DoctorProfileWriteRepositoryAdapter(
                cacheHelper,
                proxy(DoctorMapper.class, Map.of(
                        "selectOne", arguments -> existingDoctor,
                        "updateById", arguments -> {
                            updatedDoctor.value = (DoctorDO) arguments[0];
                            return 1;
                        })));

        adapter.updateByUserId(2001L, new DoctorProfileDraft("Chief Physician", "Experienced"));

        assertEquals(existingDoctor.getId(), updatedDoctor.value.getId());
        assertEquals(existingDoctor.getVersion(), updatedDoctor.value.getVersion());
        assertEquals("Chief Physician", updatedDoctor.value.getProfessionalTitle());
        assertEquals("Experienced", updatedDoctor.value.getIntroductionMasked());
        assertEquals("user:doctor-profile:2001", cacheHelper.lastDeletedKey);
    }

    @Test
    void updateByUserId_WhenFieldsCleared_PassNullValuesToUpdateById() {
        StubRedisJsonCacheHelper cacheHelper = new StubRedisJsonCacheHelper();
        DoctorDO existingDoctor = createDoctorDO();
        CapturedValue<DoctorDO> updatedDoctor = new CapturedValue<>();
        DoctorProfileWriteRepositoryAdapter adapter = new DoctorProfileWriteRepositoryAdapter(
                cacheHelper,
                proxy(DoctorMapper.class, Map.of(
                        "selectOne", arguments -> existingDoctor,
                        "updateById", arguments -> {
                            updatedDoctor.value = (DoctorDO) arguments[0];
                            return 1;
                        })));

        adapter.updateByUserId(2001L, new DoctorProfileDraft("   ", null));

        assertEquals(existingDoctor.getId(), updatedDoctor.value.getId());
        assertEquals(existingDoctor.getVersion(), updatedDoctor.value.getVersion());
        assertNull(updatedDoctor.value.getProfessionalTitle());
        assertNull(updatedDoctor.value.getIntroductionMasked());
    }

    @Test
    void updateByUserId_WhenProfileMissing_ThrowBizExceptionAndKeepCache() {
        StubRedisJsonCacheHelper cacheHelper = new StubRedisJsonCacheHelper();
        DoctorProfileWriteRepositoryAdapter adapter = new DoctorProfileWriteRepositoryAdapter(
                cacheHelper,
                proxy(DoctorMapper.class, Map.of("selectOne", arguments -> null)));

        BizException exception = assertThrows(
                BizException.class,
                () -> adapter.updateByUserId(2001L, new DoctorProfileDraft("Chief Physician", null)));

        assertEquals(UserErrorCode.DOCTOR_PROFILE_NOT_FOUND.getCode(), exception.getCode());
        assertNull(cacheHelper.lastDeletedKey);
    }

    @Test
    void updateByUserId_WhenOptimisticLockConflict_ThrowBizExceptionAndKeepCache() {
        StubRedisJsonCacheHelper cacheHelper = new StubRedisJsonCacheHelper();
        DoctorDO existingDoctor = createDoctorDO();
        DoctorProfileWriteRepositoryAdapter adapter = new DoctorProfileWriteRepositoryAdapter(
                cacheHelper,
                proxy(DoctorMapper.class, Map.of(
                        "selectOne", arguments -> existingDoctor,
                        "updateById", arguments -> 0)));

        BizException exception = assertThrows(
                BizException.class,
                () -> adapter.updateByUserId(2001L, new DoctorProfileDraft("Chief Physician", null)));

        assertEquals(UserErrorCode.DOCTOR_PROFILE_UPDATE_CONFLICT.getCode(), exception.getCode());
        assertNull(cacheHelper.lastDeletedKey);
    }

    @Test
    void updateByUserId_WhenDatabaseWriteFails_PropagateExceptionAndKeepCache() {
        StubRedisJsonCacheHelper cacheHelper = new StubRedisJsonCacheHelper();
        DoctorDO existingDoctor = createDoctorDO();
        DoctorProfileWriteRepositoryAdapter adapter = new DoctorProfileWriteRepositoryAdapter(
                cacheHelper,
                proxy(DoctorMapper.class, Map.of(
                        "selectOne", arguments -> existingDoctor,
                        "updateById", arguments -> {
                            throw new SysException(ErrorCode.SYSTEM_ERROR, "db write failed");
                        })));

        assertThrows(SysException.class, () -> adapter.updateByUserId(2001L, new DoctorProfileDraft("Chief Physician", null)));
        assertNull(cacheHelper.lastDeletedKey);
    }

    @Test
    void updateByUserId_WhenCacheDeleteFails_PropagateException() {
        StubRedisJsonCacheHelper cacheHelper = new StubRedisJsonCacheHelper();
        cacheHelper.deleteException = new SysException(ErrorCode.SYSTEM_ERROR, "cache delete failed");
        DoctorDO existingDoctor = createDoctorDO();
        DoctorProfileWriteRepositoryAdapter adapter = new DoctorProfileWriteRepositoryAdapter(
                cacheHelper,
                proxy(DoctorMapper.class, Map.of(
                        "selectOne", arguments -> existingDoctor,
                        "updateById", arguments -> 1)));

        assertThrows(SysException.class, () -> adapter.updateByUserId(2001L, new DoctorProfileDraft("Chief Physician", null)));
        assertEquals("user:doctor-profile:2001", cacheHelper.lastDeletedKey);
    }

    private static DoctorDO createDoctorDO() {
        DoctorDO doctorDO = new DoctorDO();
        doctorDO.setId(3301L);
        doctorDO.setUserId(2001L);
        doctorDO.setVersion(3);
        doctorDO.setStatus("ACTIVE");
        return doctorDO;
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

        private String lastDeletedKey;
        private SysException deleteException;

        private StubRedisJsonCacheHelper() {
            super(null, null);
        }

        @Override
        public void delete(String key) {
            this.lastDeletedKey = key;
            if (deleteException != null) {
                throw deleteException;
            }
        }
    }

    private static final class CapturedValue<T> {

        private T value;
    }
}
