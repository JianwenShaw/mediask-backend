package me.jianwen.mediask.infra.persistence.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import me.jianwen.mediask.domain.user.model.AccountStatus;
import me.jianwen.mediask.domain.user.model.AuthenticatedUser;
import me.jianwen.mediask.domain.user.model.RoleCode;
import me.jianwen.mediask.infra.persistence.mapper.ActiveRoleRow;
import me.jianwen.mediask.infra.persistence.dataobject.PatientProfileDO;
import me.jianwen.mediask.infra.persistence.dataobject.UserDO;
import me.jianwen.mediask.infra.persistence.mapper.DoctorDepartmentRelationMapper;
import me.jianwen.mediask.infra.persistence.mapper.DoctorMapper;
import me.jianwen.mediask.infra.persistence.mapper.PatientProfileMapper;
import me.jianwen.mediask.infra.persistence.mapper.PermissionMapper;
import me.jianwen.mediask.infra.persistence.mapper.RoleMapper;
import me.jianwen.mediask.infra.persistence.mapper.UserMapper;
import org.junit.jupiter.api.Test;

class UserAuthenticationRepositoryAdapterTest {

    private static final Long USER_ID = 2003L;
    private static final Long PATIENT_ID = 2201L;

    @Test
    void findAuthenticatedUserById_WhenAccountDisabledOrLocked_ReturnEmpty() {
        for (AccountStatus accountStatus : List.of(AccountStatus.DISABLED, AccountStatus.LOCKED)) {
            UserAuthenticationRepositoryAdapter adapter = newAdapter(createUserDO(accountStatus));

            assertTrue(
                    adapter.findAuthenticatedUserById(USER_ID).isEmpty(),
                    "Expected account status " + accountStatus + " to be excluded from /auth/me");
        }
    }

    @Test
    void findAuthenticatedUserById_WhenAccountActive_ReturnAuthenticatedUser() {
        UserAuthenticationRepositoryAdapter adapter = newAdapter(createUserDO(AccountStatus.ACTIVE));

        AuthenticatedUser authenticatedUser =
                adapter.findAuthenticatedUserById(USER_ID).orElseThrow();

        assertEquals(USER_ID, authenticatedUser.userId());
        assertEquals("patient_li", authenticatedUser.username());
        assertEquals("李患者", authenticatedUser.displayName());
        assertEquals(PATIENT_ID, authenticatedUser.patientId());
        assertTrue(authenticatedUser.hasRole(RoleCode.PATIENT));
        assertTrue(authenticatedUser.hasPermission("patient:profile:view:self"));
        assertFalse(authenticatedUser.roles().isEmpty());
    }

    private UserAuthenticationRepositoryAdapter newAdapter(UserDO userDO) {
        PatientProfileDO patientProfileDO = new PatientProfileDO();
        patientProfileDO.setId(PATIENT_ID);
        patientProfileDO.setUserId(USER_ID);
        ActiveRoleRow activeRoleRow = new ActiveRoleRow();
        activeRoleRow.setRoleId(1001L);
        activeRoleRow.setRoleCode("PATIENT");
        return new UserAuthenticationRepositoryAdapter(
                proxy(UserMapper.class, Map.of("selectOne", arguments -> userDO)),
                proxy(RoleMapper.class, Map.of("selectActiveRolesByUserId", arguments -> List.of(activeRoleRow))),
                proxy(
                        PermissionMapper.class,
                        Map.of("selectActivePermissionCodesByRoleIds", arguments -> List.of("patient:profile:view:self"))),
                proxy(PatientProfileMapper.class, Map.of("selectOne", arguments -> patientProfileDO)),
                proxy(DoctorMapper.class, Map.of("selectOne", arguments -> null)),
                proxy(DoctorDepartmentRelationMapper.class, Map.of("selectPrimaryDepartmentIdByDoctorId", arguments -> null)));
    }

    private UserDO createUserDO(AccountStatus accountStatus) {
        UserDO userDO = new UserDO();
        userDO.setId(USER_ID);
        userDO.setUsername("patient_li");
        userDO.setDisplayName("李患者");
        userDO.setUserType("PATIENT");
        userDO.setAccountStatus(accountStatus.name());
        userDO.setPasswordHash("hash:patient123");
        return userDO;
    }

    private static <T> T proxy(Class<T> type, Map<String, Function<Object[], Object>> handlers) {
        Object proxyInstance = Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] {type}, (proxy, method, args) -> {
            if (method.getDeclaringClass() == Object.class) {
                return handleObjectMethod(proxy, method.getName(), args);
            }
            Function<Object[], Object> handler = handlers.get(method.getName());
            assertNotNull(handler, () -> "No test handler registered for " + type.getSimpleName() + "#" + method.getName());
            return handler.apply(args == null ? new Object[0] : args);
        });
        return type.cast(proxyInstance);
    }

    private static Object handleObjectMethod(Object proxy, String methodName, Object[] args) {
        return switch (methodName) {
            case "equals" -> proxy == args[0];
            case "hashCode" -> System.identityHashCode(proxy);
            case "toString" -> proxy.getClass().getInterfaces()[0].getSimpleName() + "TestProxy";
            default -> throw new UnsupportedOperationException(methodName);
        };
    }
}
