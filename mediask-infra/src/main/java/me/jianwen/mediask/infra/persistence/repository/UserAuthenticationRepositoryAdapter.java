package me.jianwen.mediask.infra.persistence.repository;

import java.util.List;
import java.util.Optional;
import me.jianwen.mediask.domain.user.model.AccountStatus;
import me.jianwen.mediask.domain.user.model.AuthenticatedUser;
import me.jianwen.mediask.domain.user.model.LoginAccount;
import me.jianwen.mediask.domain.user.port.UserAuthenticationRepository;
import me.jianwen.mediask.infra.persistence.converter.UserAuthenticationConverter;
import me.jianwen.mediask.infra.persistence.dataobject.DoctorDO;
import me.jianwen.mediask.infra.persistence.dataobject.PatientProfileDO;
import me.jianwen.mediask.infra.persistence.dataobject.UserDO;
import me.jianwen.mediask.infra.persistence.mapper.DoctorDepartmentRelationMapper;
import me.jianwen.mediask.infra.persistence.mapper.DoctorMapper;
import me.jianwen.mediask.infra.persistence.mapper.PatientProfileMapper;
import me.jianwen.mediask.infra.persistence.mapper.RoleMapper;
import me.jianwen.mediask.infra.persistence.mapper.UserMapper;
import org.springframework.stereotype.Component;

@Component
public class UserAuthenticationRepositoryAdapter implements UserAuthenticationRepository {

    private final UserMapper userMapper;
    private final RoleMapper roleMapper;
    private final PatientProfileMapper patientProfileMapper;
    private final DoctorMapper doctorMapper;
    private final DoctorDepartmentRelationMapper doctorDepartmentRelationMapper;
    private final UserAuthenticationConverter userAuthenticationConverter = new UserAuthenticationConverter();

    public UserAuthenticationRepositoryAdapter(
            UserMapper userMapper,
            RoleMapper roleMapper,
            PatientProfileMapper patientProfileMapper,
            DoctorMapper doctorMapper,
            DoctorDepartmentRelationMapper doctorDepartmentRelationMapper) {
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
        this.patientProfileMapper = patientProfileMapper;
        this.doctorMapper = doctorMapper;
        this.doctorDepartmentRelationMapper = doctorDepartmentRelationMapper;
    }

    @Override
    public Optional<LoginAccount> findLoginAccountByUsername(String username) {
        UserDO userDO = userMapper.selectActiveByUsername(username);
        return Optional.ofNullable(userDO).map(this::toLoginAccount);
    }

    @Override
    public Optional<AuthenticatedUser> findAuthenticatedUserById(Long userId) {
        return Optional.ofNullable(userMapper.selectActiveById(userId))
                .filter(this::isCurrentUserAccessible)
                .map(this::toAuthenticatedUser);
    }

    @Override
    public void updateLastLoginAt(Long userId) {
        userMapper.updateLastLoginAt(userId);
    }

    private LoginAccount toLoginAccount(UserDO userDO) {
        UserIdentityBinding identityBinding = resolveIdentityBinding(userDO.getId());
        List<String> roleCodes = loadRoleCodes(userDO.getId());
        return userAuthenticationConverter.toLoginAccount(
                userDO,
                roleCodes,
                identityBinding.patientId(),
                identityBinding.doctorId(),
                identityBinding.primaryDepartmentId());
    }

    private AuthenticatedUser toAuthenticatedUser(UserDO userDO) {
        UserIdentityBinding identityBinding = resolveIdentityBinding(userDO.getId());
        List<String> roleCodes = loadRoleCodes(userDO.getId());
        return userAuthenticationConverter.toAuthenticatedUser(
                userDO,
                roleCodes,
                identityBinding.patientId(),
                identityBinding.doctorId(),
                identityBinding.primaryDepartmentId());
    }

    private boolean isCurrentUserAccessible(UserDO userDO) {
        return AccountStatus.fromCode(userDO.getAccountStatus()) == AccountStatus.ACTIVE;
    }

    private List<String> loadRoleCodes(Long userId) {
        return roleMapper.selectActiveRoleCodesByUserId(userId);
    }

    private UserIdentityBinding resolveIdentityBinding(Long userId) {
        PatientProfileDO patientProfileDO = patientProfileMapper.selectActiveByUserId(userId);
        DoctorDO doctorDO = doctorMapper.selectActiveByUserId(userId);
        Long patientId = patientProfileDO == null ? null : patientProfileDO.getId();
        Long doctorId = doctorDO == null ? null : doctorDO.getId();
        Long primaryDepartmentId = doctorId == null
                ? null
                : doctorDepartmentRelationMapper.selectPrimaryDepartmentIdByDoctorId(doctorId);
        return new UserIdentityBinding(patientId, doctorId, primaryDepartmentId);
    }

    private record UserIdentityBinding(Long patientId, Long doctorId, Long primaryDepartmentId) {
    }
}
