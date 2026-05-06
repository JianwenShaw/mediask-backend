package me.jianwen.mediask.infra.persistence.repository;

import me.jianwen.mediask.common.exception.BizException;
import me.jianwen.mediask.common.id.SnowflakeIdGenerator;
import me.jianwen.mediask.domain.user.exception.UserErrorCode;
import me.jianwen.mediask.domain.user.model.AdminDepartmentCreateDraft;
import me.jianwen.mediask.domain.user.model.AdminDepartmentDetail;
import me.jianwen.mediask.domain.user.model.AdminDepartmentUpdateDraft;
import me.jianwen.mediask.domain.user.port.AdminDepartmentWriteRepository;
import me.jianwen.mediask.infra.persistence.dataobject.DepartmentDO;
import me.jianwen.mediask.infra.persistence.mapper.DepartmentMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;

@Component
public class AdminDepartmentWriteRepositoryAdapter implements AdminDepartmentWriteRepository {

    private final DepartmentMapper departmentMapper;

    public AdminDepartmentWriteRepositoryAdapter(DepartmentMapper departmentMapper) {
        this.departmentMapper = departmentMapper;
    }

    @Override
    public AdminDepartmentDetail create(AdminDepartmentCreateDraft draft) {
        DepartmentDO dept = new DepartmentDO();
        dept.setId(SnowflakeIdGenerator.nextId());
        dept.setHospitalId(draft.hospitalId());
        dept.setDeptCode("DEPT_" + SnowflakeIdGenerator.nextId());
        dept.setName(draft.name());
        dept.setDeptType(draft.deptType());
        dept.setSortOrder(0);
        dept.setStatus("ACTIVE");
        dept.setVersion(0);

        try {
            departmentMapper.insert(dept);
        } catch (DuplicateKeyException exception) {
            String message = exception.getMessage();
            if (message != null && message.contains("uk_departments_code")) {
                throw new BizException(UserErrorCode.ADMIN_DEPARTMENT_CODE_CONFLICT);
            }
            throw exception;
        }

        return toDetail(dept);
    }

    @Override
    public AdminDepartmentDetail update(Long id, AdminDepartmentUpdateDraft draft) {
        DepartmentDO existing = departmentMapper.selectById(id);
        if (existing == null) {
            throw new BizException(UserErrorCode.ADMIN_DEPARTMENT_NOT_FOUND);
        }

        DepartmentDO toUpdate = new DepartmentDO();
        toUpdate.setId(id);
        toUpdate.setVersion(existing.getVersion());
        toUpdate.setName(draft.name());
        toUpdate.setDeptType(draft.deptType());
        toUpdate.setStatus(draft.status());
        toUpdate.setSortOrder(draft.sortOrder());

        int updated = departmentMapper.updateById(toUpdate);
        if (updated != 1) {
            throw new BizException(UserErrorCode.ADMIN_DEPARTMENT_UPDATE_CONFLICT);
        }

        return getRequiredDetail(id);
    }

    @Override
    public void softDelete(Long id) {
        DepartmentDO existing = departmentMapper.selectById(id);
        if (existing == null) {
            throw new BizException(UserErrorCode.ADMIN_DEPARTMENT_NOT_FOUND);
        }

        DepartmentDO toDelete = new DepartmentDO();
        toDelete.setId(id);
        toDelete.setVersion(existing.getVersion());

        int deleted = departmentMapper.deleteById(toDelete);
        if (deleted != 1) {
            throw new BizException(UserErrorCode.ADMIN_DEPARTMENT_NOT_FOUND);
        }
    }

    private AdminDepartmentDetail getRequiredDetail(Long id) {
        DepartmentDO dept = departmentMapper.selectById(id);
        if (dept == null) {
            throw new BizException(UserErrorCode.ADMIN_DEPARTMENT_NOT_FOUND);
        }
        return toDetail(dept);
    }

    private AdminDepartmentDetail toDetail(DepartmentDO dept) {
        return new AdminDepartmentDetail(
                dept.getId(),
                dept.getHospitalId(),
                dept.getDeptCode(),
                dept.getName(),
                dept.getDeptType(),
                dept.getSortOrder(),
                dept.getStatus());
    }
}
