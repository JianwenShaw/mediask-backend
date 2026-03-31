package me.jianwen.mediask.infra.persistence.repository;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import java.time.OffsetDateTime;
import me.jianwen.mediask.common.cache.CacheKeyGenerator;
import me.jianwen.mediask.common.exception.BizException;
import me.jianwen.mediask.common.id.SnowflakeIdGenerator;
import me.jianwen.mediask.domain.user.exception.UserErrorCode;
import me.jianwen.mediask.domain.user.model.AccountStatus;
import me.jianwen.mediask.domain.user.model.AdminPatientCreateDraft;
import me.jianwen.mediask.domain.user.model.AdminPatientDetail;
import me.jianwen.mediask.domain.user.model.AdminPatientUpdateDraft;
import me.jianwen.mediask.domain.user.model.RoleCode;
import me.jianwen.mediask.domain.user.model.UserType;
import me.jianwen.mediask.domain.user.port.AdminPatientWriteRepository;
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
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;

@Component
public class AdminPatientWriteRepositoryAdapter implements AdminPatientWriteRepository {

    private static final String PATIENT_NO_PREFIX = "P";

    private final RedisJsonCacheHelper redisJsonCacheHelper;
    private final UserMapper userMapper;
    private final PatientProfileMapper patientProfileMapper;
    private final RoleMapper roleMapper;
    private final UserRoleMapper userRoleMapper;

    public AdminPatientWriteRepositoryAdapter(
            RedisJsonCacheHelper redisJsonCacheHelper,
            UserMapper userMapper,
            PatientProfileMapper patientProfileMapper,
            RoleMapper roleMapper,
            UserRoleMapper userRoleMapper) {
        this.redisJsonCacheHelper = redisJsonCacheHelper;
        this.userMapper = userMapper;
        this.patientProfileMapper = patientProfileMapper;
        this.roleMapper = roleMapper;
        this.userRoleMapper = userRoleMapper;
    }

    @Override
    public AdminPatientDetail create(AdminPatientCreateDraft draft) {
        RoleDO patientRole = roleMapper.selectOne(Wrappers.lambdaQuery(RoleDO.class)
                .eq(RoleDO::getRoleCode, RoleCode.PATIENT.name())
                .eq(RoleDO::getStatus, "ACTIVE")
                .isNull(RoleDO::getDeletedAt));
        if (patientRole == null) {
            throw new BizException(UserErrorCode.ADMIN_PATIENT_ROLE_NOT_FOUND);
        }

        long userId = SnowflakeIdGenerator.nextId();
        long patientId = SnowflakeIdGenerator.nextId();
        UserDO userDO = new UserDO();
        userDO.setId(userId);
        userDO.setUsername(draft.username());
        userDO.setPasswordHash(draft.passwordHash());
        userDO.setDisplayName(draft.displayName());
        userDO.setMobileMasked(draft.mobileMasked());
        userDO.setUserType(UserType.PATIENT.name());
        userDO.setAccountStatus(AccountStatus.ACTIVE.name());
        userDO.setVersion(0);

        PatientProfileDO patientProfileDO = new PatientProfileDO();
        patientProfileDO.setId(patientId);
        patientProfileDO.setUserId(userId);
        patientProfileDO.setPatientNo(generatePatientNo());
        patientProfileDO.setGender(draft.gender());
        patientProfileDO.setBirthDate(draft.birthDate());
        patientProfileDO.setBloodType(draft.bloodType());
        patientProfileDO.setAllergySummary(draft.allergySummary());
        patientProfileDO.setVersion(0);

        UserRoleDO userRoleDO = new UserRoleDO();
        userRoleDO.setId(SnowflakeIdGenerator.nextId());
        userRoleDO.setUserId(userId);
        userRoleDO.setRoleId(patientRole.getId());
        userRoleDO.setActiveFlag(Boolean.TRUE);

        try {
            userMapper.insert(userDO);
            patientProfileMapper.insert(patientProfileDO);
            userRoleMapper.insert(userRoleDO);
        } catch (DuplicateKeyException exception) {
            throw mapDuplicateKeyOnCreate(exception);
        }

        return getRequiredDetail(patientId);
    }

    @Override
    public AdminPatientDetail update(Long patientId, AdminPatientUpdateDraft draft) {
        AdminPatientRow existingRow = patientProfileMapper.selectAdminPatientByPatientId(patientId);
        if (existingRow == null) {
            throw new BizException(UserErrorCode.ADMIN_PATIENT_NOT_FOUND);
        }

        UserDO userToUpdate = new UserDO();
        userToUpdate.setId(existingRow.getUserId());
        userToUpdate.setVersion(existingRow.getUserVersion());
        userToUpdate.setDisplayName(draft.displayName());
        userToUpdate.setMobileMasked(draft.mobileMasked());

        PatientProfileDO patientProfileToUpdate = new PatientProfileDO();
        patientProfileToUpdate.setId(existingRow.getPatientId());
        patientProfileToUpdate.setVersion(existingRow.getPatientProfileVersion());
        patientProfileToUpdate.setGender(draft.gender());
        patientProfileToUpdate.setBirthDate(draft.birthDate());
        patientProfileToUpdate.setBloodType(draft.bloodType());
        patientProfileToUpdate.setAllergySummary(draft.allergySummary());

        int updatedUsers = userMapper.updateById(userToUpdate);
        int updatedProfiles = patientProfileMapper.updateById(patientProfileToUpdate);
        if (updatedUsers != 1 || updatedProfiles != 1) {
            throw new BizException(UserErrorCode.ADMIN_PATIENT_UPDATE_CONFLICT);
        }

        redisJsonCacheHelper.delete(CacheKeyGenerator.patientProfileByUserId(existingRow.getUserId()));
        return getRequiredDetail(patientId);
    }

    @Override
    public void softDelete(Long patientId) {
        AdminPatientRow existingRow = patientProfileMapper.selectAdminPatientByPatientId(patientId);
        if (existingRow == null) {
            throw new BizException(UserErrorCode.ADMIN_PATIENT_NOT_FOUND);
        }

        OffsetDateTime deletedAt = OffsetDateTime.now();

        UserDO userToDelete = new UserDO();
        userToDelete.setId(existingRow.getUserId());
        userToDelete.setVersion(existingRow.getUserVersion());
        userToDelete.setDeletedAt(deletedAt);

        PatientProfileDO patientProfileToDelete = new PatientProfileDO();
        patientProfileToDelete.setId(existingRow.getPatientId());
        patientProfileToDelete.setVersion(existingRow.getPatientProfileVersion());
        patientProfileToDelete.setDeletedAt(deletedAt);

        int updatedUsers = userMapper.updateById(userToDelete);
        int updatedProfiles = patientProfileMapper.updateById(patientProfileToDelete);
        if (updatedUsers != 1 || updatedProfiles != 1) {
            throw new BizException(UserErrorCode.ADMIN_PATIENT_DELETE_CONFLICT);
        }

        redisJsonCacheHelper.delete(CacheKeyGenerator.patientProfileByUserId(existingRow.getUserId()));
    }

    private BizException mapDuplicateKeyOnCreate(DuplicateKeyException exception) {
        String message = exception.getMessage();
        if (message != null && message.contains("uk_users_username")) {
            return new BizException(UserErrorCode.ADMIN_PATIENT_USERNAME_CONFLICT);
        }
        if (message != null && message.contains("uk_patient_profile_no")) {
            return new BizException(UserErrorCode.ADMIN_PATIENT_NO_CONFLICT);
        }
        throw exception;
    }

    private AdminPatientDetail getRequiredDetail(Long patientId) {
        AdminPatientRow row = patientProfileMapper.selectAdminPatientByPatientId(patientId);
        if (row == null) {
            throw new BizException(UserErrorCode.ADMIN_PATIENT_NOT_FOUND);
        }
        return new AdminPatientDetail(
                row.getPatientId(),
                row.getUserId(),
                row.getPatientNo(),
                row.getUsername(),
                row.getDisplayName(),
                row.getMobileMasked(),
                row.getGender(),
                row.getBirthDate(),
                row.getBloodType(),
                row.getAllergySummary(),
                row.getAccountStatus());
    }

    private String generatePatientNo() {
        return PATIENT_NO_PREFIX + SnowflakeIdGenerator.nextId();
    }
}
