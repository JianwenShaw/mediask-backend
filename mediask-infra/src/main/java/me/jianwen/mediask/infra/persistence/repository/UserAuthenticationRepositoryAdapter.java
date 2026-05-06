package me.jianwen.mediask.infra.persistence.repository;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import me.jianwen.mediask.domain.user.model.AccountStatus;
import me.jianwen.mediask.domain.user.model.AuthenticatedUser;
import me.jianwen.mediask.domain.user.model.DataScopeRule;
import me.jianwen.mediask.domain.user.model.DataScopeType;
import me.jianwen.mediask.domain.user.model.LoginAccount;
import me.jianwen.mediask.domain.user.port.UserAuthenticationRepository;
import me.jianwen.mediask.infra.persistence.converter.UserAuthenticationConverter;
import me.jianwen.mediask.infra.persistence.dataobject.DoctorDO;
import me.jianwen.mediask.infra.persistence.dataobject.PatientProfileDO;
import me.jianwen.mediask.infra.persistence.dataobject.UserDO;
import me.jianwen.mediask.infra.persistence.mapper.ActiveDataScopeRuleRow;
import me.jianwen.mediask.infra.persistence.mapper.ActiveRoleRow;
import me.jianwen.mediask.infra.persistence.mapper.DataScopeRuleMapper;
import me.jianwen.mediask.infra.persistence.mapper.DoctorDepartmentRelationMapper;
import me.jianwen.mediask.infra.persistence.mapper.DoctorMapper;
import me.jianwen.mediask.infra.persistence.mapper.PermissionMapper;
import me.jianwen.mediask.infra.persistence.mapper.PatientProfileMapper;
import me.jianwen.mediask.infra.persistence.mapper.RoleMapper;
import me.jianwen.mediask.infra.persistence.mapper.UserMapper;
import org.springframework.stereotype.Component;

@Component
public class UserAuthenticationRepositoryAdapter implements UserAuthenticationRepository {

    private final UserMapper userMapper;
    private final RoleMapper roleMapper;
    private final PermissionMapper permissionMapper;
    private final DataScopeRuleMapper dataScopeRuleMapper;
    private final PatientProfileMapper patientProfileMapper;
    private final DoctorMapper doctorMapper;
    private final DoctorDepartmentRelationMapper doctorDepartmentRelationMapper;
    private final UserAuthenticationConverter userAuthenticationConverter = new UserAuthenticationConverter();

    public UserAuthenticationRepositoryAdapter(
            UserMapper userMapper,
            RoleMapper roleMapper,
            PermissionMapper permissionMapper,
            DataScopeRuleMapper dataScopeRuleMapper,
            PatientProfileMapper patientProfileMapper,
            DoctorMapper doctorMapper,
            DoctorDepartmentRelationMapper doctorDepartmentRelationMapper) {
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
        this.permissionMapper = permissionMapper;
        this.dataScopeRuleMapper = dataScopeRuleMapper;
        this.patientProfileMapper = patientProfileMapper;
        this.doctorMapper = doctorMapper;
        this.doctorDepartmentRelationMapper = doctorDepartmentRelationMapper;
    }

    @Override
    public Optional<LoginAccount> findLoginAccountByPhone(String phone) {
        UserDO userDO = userMapper.selectOne(Wrappers.lambdaQuery(UserDO.class)
                .eq(UserDO::getPhone, phone)
                .isNull(UserDO::getDeletedAt));
        return Optional.ofNullable(userDO).map(this::toLoginAccount);
    }

    @Override
    public Optional<AuthenticatedUser> findAuthenticatedUserById(Long userId) {
        return Optional.ofNullable(userMapper.selectOne(Wrappers.lambdaQuery(UserDO.class)
                        .eq(UserDO::getId, userId)
                        .eq(UserDO::getAccountStatus, AccountStatus.ACTIVE.name())
                        .isNull(UserDO::getDeletedAt)))
                .filter(this::isCurrentUserAccessible)
                .map(this::toAuthenticatedUser);
    }

    @Override
    public void updateLastLoginAt(Long userId) {
        OffsetDateTime now = OffsetDateTime.now();
        userMapper.update(
                null,
                Wrappers.lambdaUpdate(UserDO.class)
                        .eq(UserDO::getId, userId)
                        .isNull(UserDO::getDeletedAt)
                        .set(UserDO::getLastLoginAt, now)
                        .set(UserDO::getUpdatedAt, now));
    }

    private LoginAccount toLoginAccount(UserDO userDO) {
        UserIdentityBinding identityBinding = resolveIdentityBinding(userDO.getId());
        ActiveAuthorization activeAuthorization = loadAuthorization(userDO.getId());
        return userAuthenticationConverter.toLoginAccount(
                userDO,
                activeAuthorization.roleCodes(),
                activeAuthorization.permissionCodes(),
                activeAuthorization.dataScopeRules(),
                identityBinding.patientId(),
                identityBinding.doctorId(),
                identityBinding.primaryDepartmentId());
    }

    private AuthenticatedUser toAuthenticatedUser(UserDO userDO) {
        UserIdentityBinding identityBinding = resolveIdentityBinding(userDO.getId());
        ActiveAuthorization activeAuthorization = loadAuthorization(userDO.getId());
        return userAuthenticationConverter.toAuthenticatedUser(
                userDO,
                activeAuthorization.roleCodes(),
                activeAuthorization.permissionCodes(),
                activeAuthorization.dataScopeRules(),
                identityBinding.patientId(),
                identityBinding.doctorId(),
                identityBinding.primaryDepartmentId());
    }

    private boolean isCurrentUserAccessible(UserDO userDO) {
        return AccountStatus.fromCode(userDO.getAccountStatus()) == AccountStatus.ACTIVE;
    }

    private ActiveAuthorization loadAuthorization(Long userId) {
        List<ActiveRoleRow> activeRoles = roleMapper.selectActiveRolesByUserId(userId);
        if (activeRoles == null || activeRoles.isEmpty()) {
            return new ActiveAuthorization(List.of(), List.of(), Set.of());
        }
        List<String> roleCodes = activeRoles.stream()
                .map(ActiveRoleRow::getRoleCode)
                .filter(java.util.Objects::nonNull)
                .toList();
        List<Long> roleIds = activeRoles.stream()
                .map(ActiveRoleRow::getRoleId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();
        List<String> permissionCodes = roleIds.isEmpty()
                ? List.of()
                : permissionMapper.selectActivePermissionCodesByRoleIds(roleIds);
        Set<DataScopeRule> dataScopeRules = roleIds.isEmpty()
                ? Set.of()
                : dataScopeRuleMapper.selectActiveRulesByRoleIds(roleIds).stream()
                        .map(this::toDataScopeRule)
                        .collect(java.util.stream.Collectors.toUnmodifiableSet());
        return new ActiveAuthorization(roleCodes, permissionCodes, dataScopeRules);
    }

    private DataScopeRule toDataScopeRule(ActiveDataScopeRuleRow row) {
        return new DataScopeRule(
                row.getResourceType(),
                DataScopeType.fromCode(row.getScopeType()),
                row.getScopeDeptId());
    }

    private UserIdentityBinding resolveIdentityBinding(Long userId) {
        PatientProfileDO patientProfileDO = patientProfileMapper.selectOne(Wrappers.lambdaQuery(PatientProfileDO.class)
                .eq(PatientProfileDO::getUserId, userId)
                .isNull(PatientProfileDO::getDeletedAt));
        DoctorDO doctorDO = doctorMapper.selectOne(Wrappers.lambdaQuery(DoctorDO.class)
                .eq(DoctorDO::getUserId, userId)
                .eq(DoctorDO::getStatus, "ACTIVE")
                .isNull(DoctorDO::getDeletedAt));
        Long patientId = patientProfileDO == null ? null : patientProfileDO.getId();
        Long doctorId = doctorDO == null ? null : doctorDO.getId();
        Long primaryDepartmentId = doctorId == null
                ? null
                : doctorDepartmentRelationMapper.selectPrimaryDepartmentIdByDoctorId(doctorId);
        return new UserIdentityBinding(patientId, doctorId, primaryDepartmentId);
    }

    private record UserIdentityBinding(Long patientId, Long doctorId, Long primaryDepartmentId) {
    }

    private record ActiveAuthorization(
            List<String> roleCodes, List<String> permissionCodes, Set<DataScopeRule> dataScopeRules) {
    }
}
