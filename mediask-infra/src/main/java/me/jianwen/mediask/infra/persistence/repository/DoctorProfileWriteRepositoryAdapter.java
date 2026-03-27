package me.jianwen.mediask.infra.persistence.repository;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import me.jianwen.mediask.common.cache.CacheKeyGenerator;
import me.jianwen.mediask.common.exception.BizException;
import me.jianwen.mediask.domain.user.model.DoctorProfileDraft;
import me.jianwen.mediask.domain.user.exception.UserErrorCode;
import me.jianwen.mediask.domain.user.port.DoctorProfileWriteRepository;
import me.jianwen.mediask.infra.cache.RedisJsonCacheHelper;
import me.jianwen.mediask.infra.persistence.dataobject.DoctorDO;
import me.jianwen.mediask.infra.persistence.mapper.DoctorMapper;
import org.springframework.stereotype.Component;

@Component
public class DoctorProfileWriteRepositoryAdapter implements DoctorProfileWriteRepository {

    private final RedisJsonCacheHelper redisJsonCacheHelper;
    private final DoctorMapper doctorMapper;

    public DoctorProfileWriteRepositoryAdapter(
            RedisJsonCacheHelper redisJsonCacheHelper, DoctorMapper doctorMapper) {
        this.redisJsonCacheHelper = redisJsonCacheHelper;
        this.doctorMapper = doctorMapper;
    }

    @Override
    public void updateByUserId(Long userId, DoctorProfileDraft draft) {
        DoctorDO existingDoctor = doctorMapper.selectOne(Wrappers.lambdaQuery(DoctorDO.class)
                .eq(DoctorDO::getUserId, userId)
                .eq(DoctorDO::getStatus, "ACTIVE")
                .isNull(DoctorDO::getDeletedAt));
        if (existingDoctor == null) {
            throw new BizException(UserErrorCode.DOCTOR_PROFILE_NOT_FOUND);
        }

        DoctorDO doctorToUpdate = new DoctorDO();
        doctorToUpdate.setId(existingDoctor.getId());
        doctorToUpdate.setVersion(existingDoctor.getVersion());
        doctorToUpdate.setProfessionalTitle(draft.professionalTitle());
        doctorToUpdate.setIntroductionMasked(draft.introductionMasked());

        int updatedRows = doctorMapper.updateById(doctorToUpdate);
        if (updatedRows != 1) {
            throw new BizException(UserErrorCode.DOCTOR_PROFILE_UPDATE_CONFLICT);
        }

        // Invalidate after a successful write so readers never observe a deleted cache for a failed update.
        redisJsonCacheHelper.delete(CacheKeyGenerator.doctorProfileByUserId(userId));
    }
}
