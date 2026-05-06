package me.jianwen.mediask.infra.persistence.repository;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import me.jianwen.mediask.common.cache.CacheKeyGenerator;
import me.jianwen.mediask.common.exception.BizException;
import me.jianwen.mediask.common.id.SnowflakeIdGenerator;
import me.jianwen.mediask.domain.user.exception.UserErrorCode;
import me.jianwen.mediask.domain.user.model.AccountStatus;
import me.jianwen.mediask.domain.user.model.AdminDoctorCreateDraft;
import me.jianwen.mediask.domain.user.model.AdminDoctorDetail;
import me.jianwen.mediask.domain.user.model.AdminDoctorUpdateDraft;
import me.jianwen.mediask.domain.user.model.DoctorDepartmentAssignment;
import me.jianwen.mediask.domain.user.model.RoleCode;
import me.jianwen.mediask.domain.user.model.UserType;
import me.jianwen.mediask.domain.user.port.AdminDoctorWriteRepository;
import me.jianwen.mediask.infra.cache.RedisJsonCacheHelper;
import me.jianwen.mediask.infra.persistence.dataobject.DepartmentDO;
import me.jianwen.mediask.infra.persistence.dataobject.DoctorDO;
import me.jianwen.mediask.infra.persistence.dataobject.DoctorDepartmentRelationDO;
import me.jianwen.mediask.infra.persistence.dataobject.RoleDO;
import me.jianwen.mediask.infra.persistence.dataobject.UserDO;
import me.jianwen.mediask.infra.persistence.dataobject.UserRoleDO;
import me.jianwen.mediask.infra.persistence.mapper.AdminDoctorRow;
import me.jianwen.mediask.infra.persistence.mapper.DepartmentMapper;
import me.jianwen.mediask.infra.persistence.mapper.DoctorDepartmentRelationMapper;
import me.jianwen.mediask.infra.persistence.mapper.DoctorMapper;
import me.jianwen.mediask.infra.persistence.mapper.RoleMapper;
import me.jianwen.mediask.infra.persistence.mapper.UserMapper;
import me.jianwen.mediask.infra.persistence.mapper.UserRoleMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;

@Component
public class AdminDoctorWriteRepositoryAdapter implements AdminDoctorWriteRepository {

    private static final String DOCTOR_CODE_PREFIX = "DOC_";

    private final RedisJsonCacheHelper redisJsonCacheHelper;
    private final UserMapper userMapper;
    private final DoctorMapper doctorMapper;
    private final RoleMapper roleMapper;
    private final UserRoleMapper userRoleMapper;
    private final DepartmentMapper departmentMapper;
    private final DoctorDepartmentRelationMapper doctorDepartmentRelationMapper;

    public AdminDoctorWriteRepositoryAdapter(
            RedisJsonCacheHelper redisJsonCacheHelper,
            UserMapper userMapper,
            DoctorMapper doctorMapper,
            RoleMapper roleMapper,
            UserRoleMapper userRoleMapper,
            DepartmentMapper departmentMapper,
            DoctorDepartmentRelationMapper doctorDepartmentRelationMapper) {
        this.redisJsonCacheHelper = redisJsonCacheHelper;
        this.userMapper = userMapper;
        this.doctorMapper = doctorMapper;
        this.roleMapper = roleMapper;
        this.userRoleMapper = userRoleMapper;
        this.departmentMapper = departmentMapper;
        this.doctorDepartmentRelationMapper = doctorDepartmentRelationMapper;
    }

    @Override
    public AdminDoctorDetail create(AdminDoctorCreateDraft draft) {
        RoleDO doctorRole = roleMapper.selectOne(Wrappers.lambdaQuery(RoleDO.class)
                .eq(RoleDO::getRoleCode, RoleCode.DOCTOR.name())
                .eq(RoleDO::getStatus, "ACTIVE")
                .isNull(RoleDO::getDeletedAt));
        if (doctorRole == null) {
            throw new BizException(UserErrorCode.ADMIN_DOCTOR_ROLE_NOT_FOUND);
        }

        long userId = SnowflakeIdGenerator.nextId();
        long doctorId = SnowflakeIdGenerator.nextId();

        UserDO userDO = new UserDO();
        userDO.setId(userId);
        userDO.setUsername(draft.username());
        userDO.setPhone(draft.phone());
        userDO.setPasswordHash(draft.passwordHash());
        userDO.setDisplayName(draft.displayName());
        userDO.setUserType(UserType.DOCTOR.name());
        userDO.setAccountStatus(AccountStatus.ACTIVE.name());
        userDO.setVersion(0);

        DoctorDO doctorDO = new DoctorDO();
        doctorDO.setId(doctorId);
        doctorDO.setUserId(userId);
        doctorDO.setHospitalId(draft.hospitalId());
        doctorDO.setDoctorCode(DOCTOR_CODE_PREFIX + SnowflakeIdGenerator.nextId());
        doctorDO.setProfessionalTitle(draft.professionalTitle());
        doctorDO.setIntroductionMasked(draft.introductionMasked());
        doctorDO.setStatus("ACTIVE");
        doctorDO.setVersion(0);

        UserRoleDO userRoleDO = new UserRoleDO();
        userRoleDO.setId(SnowflakeIdGenerator.nextId());
        userRoleDO.setUserId(userId);
        userRoleDO.setRoleId(doctorRole.getId());
        userRoleDO.setActiveFlag(Boolean.TRUE);

        try {
            userMapper.insert(userDO);
            doctorMapper.insert(doctorDO);
            userRoleMapper.insert(userRoleDO);
        } catch (DuplicateKeyException exception) {
            throw mapDuplicateKeyOnCreate(exception);
        }

        insertDepartmentRelations(doctorId, draft.departmentIds());

        return getRequiredDetail(doctorId);
    }

    @Override
    public AdminDoctorDetail update(Long doctorId, AdminDoctorUpdateDraft draft) {
        AdminDoctorRow existingRow = doctorMapper.selectAdminDoctorByDoctorId(doctorId);
        if (existingRow == null) {
            throw new BizException(UserErrorCode.ADMIN_DOCTOR_NOT_FOUND);
        }

        UserDO userToUpdate = new UserDO();
        userToUpdate.setId(existingRow.getUserId());
        userToUpdate.setVersion(existingRow.getUserVersion());
        userToUpdate.setDisplayName(draft.displayName());
        userToUpdate.setPhone(draft.phone());
        userToUpdate.setMobileMasked(existingRow.getMobileMasked());

        DoctorDO doctorToUpdate = new DoctorDO();
        doctorToUpdate.setId(existingRow.getDoctorId());
        doctorToUpdate.setVersion(existingRow.getDoctorVersion());
        doctorToUpdate.setProfessionalTitle(draft.professionalTitle());
        doctorToUpdate.setIntroductionMasked(draft.introductionMasked());

        int updatedUsers = userMapper.updateById(userToUpdate);
        int updatedDoctors = doctorMapper.updateById(doctorToUpdate);
        if (updatedUsers != 1 || updatedDoctors != 1) {
            throw new BizException(UserErrorCode.ADMIN_DOCTOR_UPDATE_CONFLICT);
        }

        List<Long> currentDepartmentIds = doctorDepartmentRelationMapper
                .selectActiveByDoctorId(doctorId).stream()
                .map(DoctorDepartmentRelationDO::getDepartmentId)
                .toList();
        if (!currentDepartmentIds.equals(draft.departmentIds())) {
            replaceDepartmentRelations(doctorId, draft.departmentIds());
        }

        redisJsonCacheHelper.delete(CacheKeyGenerator.doctorProfileByUserId(existingRow.getUserId()));
        return getRequiredDetail(doctorId);
    }

    @Override
    public void softDelete(Long doctorId) {
        AdminDoctorRow existingRow = doctorMapper.selectAdminDoctorByDoctorId(doctorId);
        if (existingRow == null) {
            throw new BizException(UserErrorCode.ADMIN_DOCTOR_NOT_FOUND);
        }

        UserDO userToDelete = new UserDO();
        userToDelete.setId(existingRow.getUserId());
        userToDelete.setVersion(existingRow.getUserVersion());
        userToDelete.setMobileMasked(existingRow.getMobileMasked());

        DoctorDO doctorToDelete = new DoctorDO();
        doctorToDelete.setId(existingRow.getDoctorId());
        doctorToDelete.setVersion(existingRow.getDoctorVersion());
        doctorToDelete.setProfessionalTitle(existingRow.getProfessionalTitle());
        doctorToDelete.setIntroductionMasked(existingRow.getIntroductionMasked());

        int deletedUsers = userMapper.deleteById(userToDelete);
        int deletedDoctors = doctorMapper.deleteById(doctorToDelete);
        if (deletedUsers != 1 || deletedDoctors != 1) {
            throw new BizException(UserErrorCode.ADMIN_DOCTOR_DELETE_CONFLICT);
        }

        disableDepartmentRelations(doctorId);

        redisJsonCacheHelper.delete(CacheKeyGenerator.doctorProfileByUserId(existingRow.getUserId()));
    }

    private void insertDepartmentRelations(Long doctorId, List<Long> departmentIds) {
        if (departmentIds.isEmpty()) {
            return;
        }
        for (int i = 0; i < departmentIds.size(); i++) {
            DoctorDepartmentRelationDO relation = new DoctorDepartmentRelationDO();
            relation.setId(SnowflakeIdGenerator.nextId());
            relation.setDoctorId(doctorId);
            relation.setDepartmentId(departmentIds.get(i));
            relation.setPrimary(i == 0);
            relation.setRelationStatus("ACTIVE");
            doctorDepartmentRelationMapper.insert(relation);
        }
    }

    private void replaceDepartmentRelations(Long doctorId, List<Long> departmentIds) {
        List<DoctorDepartmentRelationDO> allExisting = doctorDepartmentRelationMapper.selectList(
                Wrappers.lambdaQuery(DoctorDepartmentRelationDO.class)
                        .eq(DoctorDepartmentRelationDO::getDoctorId, doctorId));
        Map<Long, DoctorDepartmentRelationDO> existingMap = new HashMap<>();
        for (DoctorDepartmentRelationDO rel : allExisting) {
            existingMap.put(rel.getDepartmentId(), rel);
        }

        Set<Long> newDeptIds = new HashSet<>(departmentIds);

        for (DoctorDepartmentRelationDO rel : allExisting) {
            if (newDeptIds.contains(rel.getDepartmentId())) {
                boolean shouldBePrimary = departmentIds.indexOf(rel.getDepartmentId()) == 0;
                boolean needsUpdate = !"ACTIVE".equals(rel.getRelationStatus())
                        || (shouldBePrimary != Boolean.TRUE.equals(rel.getPrimary()));
                if (needsUpdate) {
                    rel.setRelationStatus("ACTIVE");
                    rel.setPrimary(shouldBePrimary);
                    doctorDepartmentRelationMapper.updateById(rel);
                }
            } else {
                if ("ACTIVE".equals(rel.getRelationStatus())) {
                    rel.setRelationStatus("DISABLED");
                    doctorDepartmentRelationMapper.updateById(rel);
                }
            }
        }

        for (int i = 0; i < departmentIds.size(); i++) {
            Long deptId = departmentIds.get(i);
            if (!existingMap.containsKey(deptId)) {
                DoctorDepartmentRelationDO relation = new DoctorDepartmentRelationDO();
                relation.setId(SnowflakeIdGenerator.nextId());
                relation.setDoctorId(doctorId);
                relation.setDepartmentId(deptId);
                relation.setPrimary(i == 0);
                relation.setRelationStatus("ACTIVE");
                doctorDepartmentRelationMapper.insert(relation);
            }
        }
    }

    private void disableDepartmentRelations(Long doctorId) {
        List<DoctorDepartmentRelationDO> activeRelations = doctorDepartmentRelationMapper.selectList(
                Wrappers.lambdaQuery(DoctorDepartmentRelationDO.class)
                        .eq(DoctorDepartmentRelationDO::getDoctorId, doctorId)
                        .eq(DoctorDepartmentRelationDO::getRelationStatus, "ACTIVE"));
        for (DoctorDepartmentRelationDO relation : activeRelations) {
            relation.setRelationStatus("DISABLED");
            doctorDepartmentRelationMapper.updateById(relation);
        }
    }

    private BizException mapDuplicateKeyOnCreate(DuplicateKeyException exception) {
        String message = exception.getMessage();
        if (message != null && message.contains("uk_users_username")) {
            return new BizException(UserErrorCode.ADMIN_DOCTOR_USERNAME_CONFLICT);
        }
        if (message != null && message.contains("uk_users_phone")) {
            return new BizException(UserErrorCode.ADMIN_DOCTOR_PHONE_CONFLICT);
        }
        if (message != null && message.contains("uk_doctors_code")) {
            return new BizException(UserErrorCode.ADMIN_DOCTOR_CODE_CONFLICT);
        }
        throw exception;
    }

    private AdminDoctorDetail getRequiredDetail(Long doctorId) {
        AdminDoctorRow row = doctorMapper.selectAdminDoctorByDoctorId(doctorId);
        if (row == null) {
            throw new BizException(UserErrorCode.ADMIN_DOCTOR_NOT_FOUND);
        }
        List<DoctorDepartmentAssignment> departments = loadDepartmentAssignments(doctorId);
        return new AdminDoctorDetail(
                row.getDoctorId(),
                row.getUserId(),
                row.getUsername(),
                row.getDisplayName(),
                row.getPhone(),
                row.getHospitalId(),
                row.getDoctorCode(),
                row.getProfessionalTitle(),
                row.getIntroductionMasked(),
                departments,
                row.getAccountStatus());
    }

    private List<DoctorDepartmentAssignment> loadDepartmentAssignments(Long doctorId) {
        return doctorDepartmentRelationMapper.selectActiveByDoctorId(doctorId).stream()
                .map(rel -> {
                    DepartmentDO dept = departmentMapper.selectOne(
                            Wrappers.lambdaQuery(DepartmentDO.class)
                                    .eq(DepartmentDO::getId, rel.getDepartmentId())
                                    .eq(DepartmentDO::getStatus, "ACTIVE")
                                    .isNull(DepartmentDO::getDeletedAt));
                    return new DoctorDepartmentAssignment(
                            rel.getDepartmentId(),
                            dept != null ? dept.getName() : "",
                            Boolean.TRUE.equals(rel.getPrimary()));
                })
                .toList();
    }
}
