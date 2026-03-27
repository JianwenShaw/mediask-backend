package me.jianwen.mediask.infra.persistence.repository;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import java.util.Optional;
import me.jianwen.mediask.common.cache.CacheKeyGenerator;
import me.jianwen.mediask.domain.user.model.DoctorProfile;
import me.jianwen.mediask.domain.user.port.DoctorProfileRepository;
import me.jianwen.mediask.infra.cache.RedisJsonCacheHelper;
import me.jianwen.mediask.infra.cache.UserProfileCachePolicy;
import me.jianwen.mediask.infra.persistence.dataobject.DepartmentDO;
import me.jianwen.mediask.infra.persistence.dataobject.DoctorDO;
import me.jianwen.mediask.infra.persistence.mapper.DepartmentMapper;
import me.jianwen.mediask.infra.persistence.mapper.DoctorDepartmentRelationMapper;
import me.jianwen.mediask.infra.persistence.mapper.DoctorMapper;
import org.springframework.stereotype.Component;

@Component
public class DoctorProfileRepositoryAdapter implements DoctorProfileRepository {

    private final RedisJsonCacheHelper redisJsonCacheHelper;
    private final DoctorMapper doctorMapper;
    private final DoctorDepartmentRelationMapper doctorDepartmentRelationMapper;
    private final DepartmentMapper departmentMapper;

    public DoctorProfileRepositoryAdapter(
            RedisJsonCacheHelper redisJsonCacheHelper,
            DoctorMapper doctorMapper,
            DoctorDepartmentRelationMapper doctorDepartmentRelationMapper,
            DepartmentMapper departmentMapper) {
        this.redisJsonCacheHelper = redisJsonCacheHelper;
        this.doctorMapper = doctorMapper;
        this.doctorDepartmentRelationMapper = doctorDepartmentRelationMapper;
        this.departmentMapper = departmentMapper;
    }

    @Override
    public Optional<DoctorProfile> findByUserId(Long userId) {
        String cacheKey = CacheKeyGenerator.doctorProfileByUserId(userId);
        Optional<DoctorProfile> cachedProfile = redisJsonCacheHelper.get(cacheKey, DoctorProfile.class);
        if (cachedProfile.isPresent()) {
            return cachedProfile;
        }

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
        DoctorProfile doctorProfile = new DoctorProfile(
                doctorDO.getId(),
                doctorDO.getDoctorCode(),
                doctorDO.getProfessionalTitle(),
                doctorDO.getIntroductionMasked(),
                doctorDO.getHospitalId(),
                primaryDepartmentId,
                primaryDepartment == null ? null : primaryDepartment.getName());
        redisJsonCacheHelper.put(cacheKey, doctorProfile, UserProfileCachePolicy.DOCTOR_PROFILE_TTL);
        return Optional.of(doctorProfile);
    }
}
