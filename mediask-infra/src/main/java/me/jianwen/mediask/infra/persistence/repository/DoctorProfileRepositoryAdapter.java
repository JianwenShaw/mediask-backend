package me.jianwen.mediask.infra.persistence.repository;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import java.util.Optional;
import me.jianwen.mediask.domain.user.model.DoctorProfile;
import me.jianwen.mediask.domain.user.port.DoctorProfileRepository;
import me.jianwen.mediask.infra.persistence.dataobject.DepartmentDO;
import me.jianwen.mediask.infra.persistence.dataobject.DoctorDO;
import me.jianwen.mediask.infra.persistence.mapper.DepartmentMapper;
import me.jianwen.mediask.infra.persistence.mapper.DoctorDepartmentRelationMapper;
import me.jianwen.mediask.infra.persistence.mapper.DoctorMapper;
import org.springframework.stereotype.Component;

@Component
public class DoctorProfileRepositoryAdapter implements DoctorProfileRepository {

    private final DoctorMapper doctorMapper;
    private final DoctorDepartmentRelationMapper doctorDepartmentRelationMapper;
    private final DepartmentMapper departmentMapper;

    public DoctorProfileRepositoryAdapter(
            DoctorMapper doctorMapper,
            DoctorDepartmentRelationMapper doctorDepartmentRelationMapper,
            DepartmentMapper departmentMapper) {
        this.doctorMapper = doctorMapper;
        this.doctorDepartmentRelationMapper = doctorDepartmentRelationMapper;
        this.departmentMapper = departmentMapper;
    }

    @Override
    public Optional<DoctorProfile> findByUserId(Long userId) {
        DoctorDO doctorDO = doctorMapper.selectOne(Wrappers.lambdaQuery(DoctorDO.class)
                .eq(DoctorDO::getUserId, userId)
                .eq(DoctorDO::getStatus, "ACTIVE")
                .isNull(DoctorDO::getDeletedAt));
        if (doctorDO == null) {
            return Optional.empty();
        }
        Long primaryDepartmentId = doctorDepartmentRelationMapper.selectPrimaryDepartmentIdByDoctorId(doctorDO.getId());
        DepartmentDO primaryDepartment = primaryDepartmentId == null
                ? null
                : departmentMapper.selectOne(Wrappers.lambdaQuery(DepartmentDO.class)
                        .eq(DepartmentDO::getId, primaryDepartmentId)
                        .eq(DepartmentDO::getStatus, "ACTIVE")
                        .isNull(DepartmentDO::getDeletedAt));
        return Optional.of(new DoctorProfile(
                doctorDO.getId(),
                doctorDO.getDoctorCode(),
                doctorDO.getProfessionalTitle(),
                doctorDO.getIntroductionMasked(),
                doctorDO.getHospitalId(),
                primaryDepartmentId,
                primaryDepartment == null ? null : primaryDepartment.getName()));
    }
}
