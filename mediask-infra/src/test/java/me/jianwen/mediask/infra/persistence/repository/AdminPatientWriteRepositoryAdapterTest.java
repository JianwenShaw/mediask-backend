package me.jianwen.mediask.infra.persistence.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Proxy;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.function.Function;
import me.jianwen.mediask.common.exception.BizException;
import me.jianwen.mediask.common.exception.ErrorCode;
import me.jianwen.mediask.common.exception.SysException;
import me.jianwen.mediask.domain.user.exception.UserErrorCode;
import me.jianwen.mediask.domain.user.model.AdminPatientCreateDraft;
import me.jianwen.mediask.domain.user.model.AdminPatientDetail;
import me.jianwen.mediask.domain.user.model.AdminPatientUpdateDraft;
import me.jianwen.mediask.infra.cache.RedisJsonCacheHelper;
import me.jianwen.mediask.infra.persistence.dataobject.PatientProfileDO;
import me.jianwen.mediask.infra.persistence.dataobject.RoleDO;
import me.jianwen.mediask.infra.persistence.dataobject.UserDO;
import me.jianwen.mediask.infra.persistence.dataobject.UserRoleDO;
import me.jianwen.mediask.infra.persistence.mapper.AdminPatientRow;
import me.jianwen.mediask.infra.persistence.mapper.PatientProfileMapper;
import me.jianwen.mediask.infra.persistence.mapper.RoleMapper;
import me.jianwen.mediask.infra.persistence.mapper.UserMapper;
import me.jianwen.mediask.infra.persistence.mapper.UserRoleMapper;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;

class AdminPatientWriteRepositoryAdapterTest {

    @Test
    void create_WhenWriteSucceeds_ReturnDetail() {
        CapturedValue<UserDO> insertedUser = new CapturedValue<>();
        CapturedValue<PatientProfileDO> insertedProfile = new CapturedValue<>();
        CapturedValue<UserRoleDO> insertedUserRole = new CapturedValue<>();
        AdminPatientWriteRepositoryAdapter adapter = new AdminPatientWriteRepositoryAdapter(
                new StubRedisJsonCacheHelper(),
                proxy(UserMapper.class, Map.of("insert", arguments -> {
                    insertedUser.value = (UserDO) arguments[0];
                    return 1;
                })),
                proxy(PatientProfileMapper.class, Map.of(
                        "insert", arguments -> {
                            insertedProfile.value = (PatientProfileDO) arguments[0];
                            return 1;
                        },
                        "selectAdminPatientByPatientId", arguments -> row(
                                insertedProfile.value.getId(),
                                insertedUser.value.getId(),
                                insertedProfile.value.getVersion(),
                                insertedUser.value.getVersion(),
                                insertedProfile.value.getPatientNo(),
                                insertedUser.value.getUsername(),
                                insertedUser.value.getDisplayName(),
                                insertedUser.value.getMobileMasked(),
                                insertedProfile.value.getGender(),
                                insertedProfile.value.getBirthDate(),
                                insertedProfile.value.getBloodType(),
                                insertedProfile.value.getAllergySummary(),
                                insertedUser.value.getAccountStatus()))),
                proxy(RoleMapper.class, Map.of("selectOne", arguments -> role())),
                proxy(UserRoleMapper.class, Map.of("insert", arguments -> {
                    insertedUserRole.value = (UserRoleDO) arguments[0];
                    return 1;
                })));

        AdminPatientDetail detail = adapter.create(new AdminPatientCreateDraft(
                "patient_new", "hash<patient123>", "李新患者", "137****1234", "FEMALE", LocalDate.of(1995, 6, 1), "A", "Peanut"));

        assertEquals("patient_new", insertedUser.value.getUsername());
        assertEquals("hash<patient123>", insertedUser.value.getPasswordHash());
        assertTrue(insertedProfile.value.getPatientNo().startsWith("P"));
        assertEquals(role().getId(), insertedUserRole.value.getRoleId());
        assertEquals("ACTIVE", detail.accountStatus());
    }

    @Test
    void update_WhenPatientMissing_ThrowNotFound() {
        AdminPatientWriteRepositoryAdapter adapter = new AdminPatientWriteRepositoryAdapter(
                new StubRedisJsonCacheHelper(),
                proxy(UserMapper.class, Map.of()),
                proxy(PatientProfileMapper.class, Map.of("selectAdminPatientByPatientId", arguments -> null)),
                proxy(RoleMapper.class, Map.of()),
                proxy(UserRoleMapper.class, Map.of()));

        BizException exception = assertThrows(
                BizException.class,
                () -> adapter.update(2208L, new AdminPatientUpdateDraft("李修改", null, null, null, null, null)));

        assertEquals(UserErrorCode.ADMIN_PATIENT_NOT_FOUND.getCode(), exception.getCode());
    }

    @Test
    void update_WhenMobileMaskedCleared_PassNullToUpdateByIdAndDeleteCache() {
        StubRedisJsonCacheHelper cacheHelper = new StubRedisJsonCacheHelper();
        CapturedValue<UserDO> updatedUser = new CapturedValue<>();
        CapturedValue<PatientProfileDO> updatedProfile = new CapturedValue<>();
        AdminPatientWriteRepositoryAdapter adapter = new AdminPatientWriteRepositoryAdapter(
                cacheHelper,
                proxy(UserMapper.class, Map.of("updateById", arguments -> {
                    updatedUser.value = (UserDO) arguments[0];
                    return 1;
                })),
                proxy(PatientProfileMapper.class, Map.of(
                        "selectAdminPatientByPatientId", arguments -> row(2208L, 2008L, 3, 4, "P20260008", "patient_new",
                                "李原患者", "137****1234", "FEMALE", LocalDate.of(1995, 6, 1), "A", "Peanut", "ACTIVE"),
                        "updateById", arguments -> {
                            updatedProfile.value = (PatientProfileDO) arguments[0];
                            return 1;
                        })),
                proxy(RoleMapper.class, Map.of()),
                proxy(UserRoleMapper.class, Map.of()));

        AdminPatientDetail detail = adapter.update(
                2208L, new AdminPatientUpdateDraft("李修改", "   ", null, null, "   ", null));

        assertEquals(2008L, updatedUser.value.getId());
        assertEquals(4, updatedUser.value.getVersion());
        assertEquals("李修改", updatedUser.value.getDisplayName());
        assertNull(updatedUser.value.getMobileMasked());
        assertEquals(2208L, updatedProfile.value.getId());
        assertEquals(3, updatedProfile.value.getVersion());
        assertNull(updatedProfile.value.getGender());
        assertNull(updatedProfile.value.getBirthDate());
        assertNull(updatedProfile.value.getBloodType());
        assertNull(updatedProfile.value.getAllergySummary());
        assertEquals("user:patient-profile:2008", cacheHelper.lastDeletedKey);
        assertEquals("137****1234", detail.mobileMasked());
    }

    @Test
    void update_WhenWriteSucceeds_ReturnDetailAndPersistFields() {
        StubRedisJsonCacheHelper cacheHelper = new StubRedisJsonCacheHelper();
        CapturedValue<UserDO> updatedUser = new CapturedValue<>();
        CapturedValue<PatientProfileDO> updatedProfile = new CapturedValue<>();
        AdminPatientWriteRepositoryAdapter adapter = new AdminPatientWriteRepositoryAdapter(
                cacheHelper,
                proxy(UserMapper.class, Map.of("updateById", arguments -> {
                    updatedUser.value = (UserDO) arguments[0];
                    return 1;
                })),
                proxy(PatientProfileMapper.class, Map.of(
                        "selectAdminPatientByPatientId", arguments -> {
                            if (updatedUser.value == null) {
                                return row(2208L, 2008L, 3, 4, "P20260008", "patient_new", "李原患者", "137****1234",
                                        "FEMALE", LocalDate.of(1995, 6, 1), "A", "Peanut", "ACTIVE");
                            }
                            return row(2208L, 2008L, 4, 5, "P20260008", "patient_new", updatedUser.value.getDisplayName(),
                                    updatedUser.value.getMobileMasked(), updatedProfile.value.getGender(),
                                    updatedProfile.value.getBirthDate(), updatedProfile.value.getBloodType(),
                                    updatedProfile.value.getAllergySummary(), "ACTIVE");
                        },
                        "updateById", arguments -> {
                            updatedProfile.value = (PatientProfileDO) arguments[0];
                            return 1;
                        })),
                proxy(RoleMapper.class, Map.of()),
                proxy(UserRoleMapper.class, Map.of()));

        AdminPatientDetail detail = adapter.update(
                2208L, new AdminPatientUpdateDraft("李修改", "137****9999", "MALE", LocalDate.of(1990, 1, 2), "B", "Dust"));

        assertEquals(2008L, updatedUser.value.getId());
        assertEquals(4, updatedUser.value.getVersion());
        assertEquals("李修改", updatedUser.value.getDisplayName());
        assertEquals("137****9999", updatedUser.value.getMobileMasked());
        assertEquals(2208L, updatedProfile.value.getId());
        assertEquals(3, updatedProfile.value.getVersion());
        assertEquals("MALE", updatedProfile.value.getGender());
        assertEquals(LocalDate.of(1990, 1, 2), updatedProfile.value.getBirthDate());
        assertEquals("B", updatedProfile.value.getBloodType());
        assertEquals("Dust", updatedProfile.value.getAllergySummary());
        assertEquals("user:patient-profile:2008", cacheHelper.lastDeletedKey);
        assertEquals("李修改", detail.displayName());
        assertEquals("137****9999", detail.mobileMasked());
        assertEquals("MALE", detail.gender());
        assertEquals(LocalDate.of(1990, 1, 2), detail.birthDate());
        assertEquals("B", detail.bloodType());
        assertEquals("Dust", detail.allergySummary());
    }

    @Test
    void softDelete_WhenWriteSucceeds_DeleteCache() {
        StubRedisJsonCacheHelper cacheHelper = new StubRedisJsonCacheHelper();
        CapturedValue<UserDO> deletedUser = new CapturedValue<>();
        CapturedValue<PatientProfileDO> deletedProfile = new CapturedValue<>();
        AdminPatientWriteRepositoryAdapter adapter = new AdminPatientWriteRepositoryAdapter(
                cacheHelper,
                proxy(UserMapper.class, Map.of("updateById", arguments -> {
                    deletedUser.value = (UserDO) arguments[0];
                    return 1;
                })),
                proxy(PatientProfileMapper.class, Map.of(
                        "selectAdminPatientByPatientId", arguments -> row(2208L, 2008L, 3, 4, "P20260008", "patient_new",
                                "李新患者", "137****1234", "FEMALE", LocalDate.of(1995, 6, 1), "A", "Peanut", "ACTIVE"),
                        "updateById", arguments -> {
                            deletedProfile.value = (PatientProfileDO) arguments[0];
                            return 1;
                        })),
                proxy(RoleMapper.class, Map.of()),
                proxy(UserRoleMapper.class, Map.of()));

        adapter.softDelete(2208L);

        assertEquals(2008L, deletedUser.value.getId());
        assertEquals(2208L, deletedProfile.value.getId());
        assertTrue(deletedUser.value.getDeletedAt() instanceof OffsetDateTime);
        assertTrue(deletedProfile.value.getDeletedAt() instanceof OffsetDateTime);
        assertEquals("user:patient-profile:2008", cacheHelper.lastDeletedKey);
    }

    @Test
    void create_WhenUsernameConflict_ThrowBizException() {
        AdminPatientWriteRepositoryAdapter adapter = new AdminPatientWriteRepositoryAdapter(
                new StubRedisJsonCacheHelper(),
                proxy(UserMapper.class, Map.of("insert", arguments -> {
                    throw new DuplicateKeyException("duplicate key value violates unique constraint \"uk_users_username\"");
                })),
                proxy(PatientProfileMapper.class, Map.of()),
                proxy(RoleMapper.class, Map.of("selectOne", arguments -> role())),
                proxy(UserRoleMapper.class, Map.of()));

        BizException exception = assertThrows(
                BizException.class,
                () -> adapter.create(new AdminPatientCreateDraft(
                        "patient_new", "hash<patient123>", "李新患者", null, null, null, null, null)));

        assertEquals(UserErrorCode.ADMIN_PATIENT_USERNAME_CONFLICT.getCode(), exception.getCode());
    }

    @Test
    void softDelete_WhenCacheDeleteFails_PropagateException() {
        StubRedisJsonCacheHelper cacheHelper = new StubRedisJsonCacheHelper();
        cacheHelper.deleteException = new SysException(ErrorCode.SYSTEM_ERROR, "cache delete failed");
        AdminPatientWriteRepositoryAdapter adapter = new AdminPatientWriteRepositoryAdapter(
                cacheHelper,
                proxy(UserMapper.class, Map.of("updateById", arguments -> 1)),
                proxy(PatientProfileMapper.class, Map.of(
                        "selectAdminPatientByPatientId", arguments -> row(2208L, 2008L, 3, 4, "P20260008", "patient_new",
                                "李新患者", "137****1234", "FEMALE", LocalDate.of(1995, 6, 1), "A", "Peanut", "ACTIVE"),
                        "updateById", arguments -> 1)),
                proxy(RoleMapper.class, Map.of()),
                proxy(UserRoleMapper.class, Map.of()));

        assertThrows(SysException.class, () -> adapter.softDelete(2208L));
        assertEquals("user:patient-profile:2008", cacheHelper.lastDeletedKey);
    }

    private static RoleDO role() {
        RoleDO roleDO = new RoleDO();
        roleDO.setId(1001L);
        return roleDO;
    }

    private static AdminPatientRow row(
            Long patientId,
            Long userId,
            Integer patientProfileVersion,
            Integer userVersion,
            String patientNo,
            String username,
            String displayName,
            String mobileMasked,
            String gender,
            LocalDate birthDate,
            String bloodType,
            String allergySummary,
            String accountStatus) {
        AdminPatientRow row = new AdminPatientRow();
        row.setPatientId(patientId);
        row.setUserId(userId);
        row.setPatientProfileVersion(patientProfileVersion);
        row.setUserVersion(userVersion);
        row.setPatientNo(patientNo);
        row.setUsername(username);
        row.setDisplayName(displayName);
        row.setMobileMasked(mobileMasked);
        row.setGender(gender);
        row.setBirthDate(birthDate);
        row.setBloodType(bloodType);
        row.setAllergySummary(allergySummary);
        row.setAccountStatus(accountStatus);
        return row;
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
