package me.jianwen.mediask.infra.persistence.repository;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.List;
import java.util.Optional;
import me.jianwen.mediask.common.pagination.PageData;
import me.jianwen.mediask.common.pagination.PageQuery;
import me.jianwen.mediask.domain.user.model.AdminDoctorDetail;
import me.jianwen.mediask.domain.user.model.AdminDoctorListItem;
import me.jianwen.mediask.domain.user.model.DoctorDepartmentAssignment;
import me.jianwen.mediask.domain.user.port.AdminDoctorQueryRepository;
import me.jianwen.mediask.infra.persistence.dataobject.DepartmentDO;
import me.jianwen.mediask.infra.persistence.dataobject.DoctorDepartmentRelationDO;
import me.jianwen.mediask.infra.persistence.mapper.AdminDoctorRow;
import me.jianwen.mediask.infra.persistence.mapper.DepartmentMapper;
import me.jianwen.mediask.infra.persistence.mapper.DoctorDepartmentRelationMapper;
import me.jianwen.mediask.infra.persistence.mapper.DoctorMapper;
import org.springframework.stereotype.Component;

@Component
public class AdminDoctorQueryRepositoryAdapter implements AdminDoctorQueryRepository {

    private final DoctorMapper doctorMapper;
    private final DepartmentMapper departmentMapper;
    private final DoctorDepartmentRelationMapper doctorDepartmentRelationMapper;

    public AdminDoctorQueryRepositoryAdapter(
            DoctorMapper doctorMapper,
            DepartmentMapper departmentMapper,
            DoctorDepartmentRelationMapper doctorDepartmentRelationMapper) {
        this.doctorMapper = doctorMapper;
        this.departmentMapper = departmentMapper;
        this.doctorDepartmentRelationMapper = doctorDepartmentRelationMapper;
    }

    @Override
    public PageData<AdminDoctorListItem> pageByKeyword(String keyword, PageQuery pageQuery) {
        Page<AdminDoctorRow> page = new Page<>(pageQuery.pageNum(), pageQuery.pageSize(), true);
        var result = doctorMapper.selectAdminDoctorsByKeywordPage(page, keyword);
        return new PageData<>(
                result.getRecords().stream().map(this::toListItem).toList(),
                result.getCurrent(),
                result.getSize(),
                result.getTotal(),
                result.getPages(),
                result.getCurrent() < result.getPages());
    }

    @Override
    public Optional<AdminDoctorDetail> findDetailByDoctorId(Long doctorId) {
        return Optional.ofNullable(doctorMapper.selectAdminDoctorByDoctorId(doctorId))
                .map(this::toDetail);
    }

    private AdminDoctorListItem toListItem(AdminDoctorRow row) {
        return new AdminDoctorListItem(
                row.getDoctorId(),
                row.getUserId(),
                row.getUsername(),
                row.getDisplayName(),
                row.getDoctorCode(),
                row.getProfessionalTitle(),
                resolvePrimaryDepartmentName(row.getDoctorId()),
                row.getAccountStatus());
    }

    private AdminDoctorDetail toDetail(AdminDoctorRow row) {
        List<DoctorDepartmentAssignment> departments = loadDepartmentAssignments(row.getDoctorId());
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

    private String resolvePrimaryDepartmentName(Long doctorId) {
        Long primaryDeptId = doctorDepartmentRelationMapper.selectPrimaryDepartmentIdByDoctorId(doctorId);
        if (primaryDeptId == null) {
            return null;
        }
        DepartmentDO dept = departmentMapper.selectOne(
                Wrappers.lambdaQuery(DepartmentDO.class)
                        .eq(DepartmentDO::getId, primaryDeptId)
                        .eq(DepartmentDO::getStatus, "ACTIVE")
                        .isNull(DepartmentDO::getDeletedAt));
        return dept != null ? dept.getName() : null;
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
