package me.jianwen.mediask.infra.persistence.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.reflect.Proxy;
import java.time.LocalDate;
import java.util.Map;
import java.util.function.Function;
import me.jianwen.mediask.common.exception.BizException;
import me.jianwen.mediask.common.exception.ErrorCode;
import me.jianwen.mediask.common.exception.SysException;
import me.jianwen.mediask.domain.user.exception.UserErrorCode;
import me.jianwen.mediask.domain.user.model.PatientProfileDraft;
import me.jianwen.mediask.infra.cache.RedisJsonCacheHelper;
import me.jianwen.mediask.infra.persistence.dataobject.PatientProfileDO;
import me.jianwen.mediask.infra.persistence.mapper.PatientProfileMapper;
import org.junit.jupiter.api.Test;

class PatientProfileWriteRepositoryAdapterTest {

    @Test
    void updateByUserId_WhenUpdateSucceeds_DeleteCache() {
        StubRedisJsonCacheHelper cacheHelper = new StubRedisJsonCacheHelper();
        PatientProfileDO existingProfile = createPatientProfileDO();
        CapturedValue<PatientProfileDO> updatedProfile = new CapturedValue<>();
        PatientProfileWriteRepositoryAdapter adapter = new PatientProfileWriteRepositoryAdapter(
                cacheHelper,
                proxy(PatientProfileMapper.class, Map.of(
                        "selectOne", arguments -> existingProfile,
                        "updateById", arguments -> {
                            updatedProfile.value = (PatientProfileDO) arguments[0];
                            return 1;
                        })));

        adapter.updateByUserId(2001L, new PatientProfileDraft("FEMALE", LocalDate.of(1992, 5, 1), "A", "Peanut"));

        assertEquals(existingProfile.getId(), updatedProfile.value.getId());
        assertEquals(existingProfile.getVersion(), updatedProfile.value.getVersion());
        assertEquals("FEMALE", updatedProfile.value.getGender());
        assertEquals(LocalDate.of(1992, 5, 1), updatedProfile.value.getBirthDate());
        assertEquals("A", updatedProfile.value.getBloodType());
        assertEquals("Peanut", updatedProfile.value.getAllergySummary());
        assertEquals("user:patient-profile:2001", cacheHelper.lastDeletedKey);
    }

    @Test
    void updateByUserId_WhenFieldsCleared_PassNullValuesToUpdateById() {
        StubRedisJsonCacheHelper cacheHelper = new StubRedisJsonCacheHelper();
        PatientProfileDO existingProfile = createPatientProfileDO();
        CapturedValue<PatientProfileDO> updatedProfile = new CapturedValue<>();
        PatientProfileWriteRepositoryAdapter adapter = new PatientProfileWriteRepositoryAdapter(
                cacheHelper,
                proxy(PatientProfileMapper.class, Map.of(
                        "selectOne", arguments -> existingProfile,
                        "updateById", arguments -> {
                            updatedProfile.value = (PatientProfileDO) arguments[0];
                            return 1;
                        })));

        adapter.updateByUserId(2001L, new PatientProfileDraft("   ", null, "   ", null));

        assertEquals(existingProfile.getId(), updatedProfile.value.getId());
        assertEquals(existingProfile.getVersion(), updatedProfile.value.getVersion());
        assertNull(updatedProfile.value.getGender());
        assertNull(updatedProfile.value.getBirthDate());
        assertNull(updatedProfile.value.getBloodType());
        assertNull(updatedProfile.value.getAllergySummary());
    }

    @Test
    void updateByUserId_WhenProfileMissing_ThrowBizExceptionAndKeepCache() {
        StubRedisJsonCacheHelper cacheHelper = new StubRedisJsonCacheHelper();
        PatientProfileWriteRepositoryAdapter adapter = new PatientProfileWriteRepositoryAdapter(
                cacheHelper,
                proxy(PatientProfileMapper.class, Map.of("selectOne", arguments -> null)));

        BizException exception = assertThrows(
                BizException.class,
                () -> adapter.updateByUserId(2001L, new PatientProfileDraft("FEMALE", null, "A", null)));

        assertEquals(UserErrorCode.PATIENT_PROFILE_NOT_FOUND.getCode(), exception.getCode());
        assertNull(cacheHelper.lastDeletedKey);
    }

    @Test
    void updateByUserId_WhenOptimisticLockConflict_ThrowBizExceptionAndKeepCache() {
        StubRedisJsonCacheHelper cacheHelper = new StubRedisJsonCacheHelper();
        PatientProfileDO existingProfile = createPatientProfileDO();
        PatientProfileWriteRepositoryAdapter adapter = new PatientProfileWriteRepositoryAdapter(
                cacheHelper,
                proxy(PatientProfileMapper.class, Map.of(
                        "selectOne", arguments -> existingProfile,
                        "updateById", arguments -> 0)));

        BizException exception = assertThrows(
                BizException.class,
                () -> adapter.updateByUserId(2001L, new PatientProfileDraft("FEMALE", null, "A", null)));

        assertEquals(UserErrorCode.PATIENT_PROFILE_UPDATE_CONFLICT.getCode(), exception.getCode());
        assertNull(cacheHelper.lastDeletedKey);
    }

    @Test
    void updateByUserId_WhenDatabaseWriteFails_PropagateExceptionAndKeepCache() {
        StubRedisJsonCacheHelper cacheHelper = new StubRedisJsonCacheHelper();
        PatientProfileDO existingProfile = createPatientProfileDO();
        PatientProfileWriteRepositoryAdapter adapter = new PatientProfileWriteRepositoryAdapter(
                cacheHelper,
                proxy(PatientProfileMapper.class, Map.of(
                        "selectOne", arguments -> existingProfile,
                        "updateById", arguments -> {
                            throw new SysException(ErrorCode.SYSTEM_ERROR, "db write failed");
                        })));

        assertThrows(
                SysException.class,
                () -> adapter.updateByUserId(2001L, new PatientProfileDraft("FEMALE", null, "A", null)));
        assertNull(cacheHelper.lastDeletedKey);
    }

    @Test
    void updateByUserId_WhenCacheDeleteFails_PropagateException() {
        StubRedisJsonCacheHelper cacheHelper = new StubRedisJsonCacheHelper();
        cacheHelper.deleteException = new SysException(ErrorCode.SYSTEM_ERROR, "cache delete failed");
        PatientProfileDO existingProfile = createPatientProfileDO();
        PatientProfileWriteRepositoryAdapter adapter = new PatientProfileWriteRepositoryAdapter(
                cacheHelper,
                proxy(PatientProfileMapper.class, Map.of(
                        "selectOne", arguments -> existingProfile,
                        "updateById", arguments -> 1)));

        assertThrows(
                SysException.class,
                () -> adapter.updateByUserId(2001L, new PatientProfileDraft("FEMALE", null, "A", null)));
        assertEquals("user:patient-profile:2001", cacheHelper.lastDeletedKey);
    }

    private static PatientProfileDO createPatientProfileDO() {
        PatientProfileDO patientProfileDO = new PatientProfileDO();
        patientProfileDO.setId(2201L);
        patientProfileDO.setUserId(2001L);
        patientProfileDO.setVersion(5);
        return patientProfileDO;
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
