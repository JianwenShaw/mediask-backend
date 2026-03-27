package me.jianwen.mediask.infra.persistence.repository;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import me.jianwen.mediask.common.cache.CacheKeyGenerator;
import me.jianwen.mediask.common.exception.BizException;
import me.jianwen.mediask.domain.user.exception.UserErrorCode;
import me.jianwen.mediask.domain.user.model.PatientProfileDraft;
import me.jianwen.mediask.domain.user.port.PatientProfileWriteRepository;
import me.jianwen.mediask.infra.cache.RedisJsonCacheHelper;
import me.jianwen.mediask.infra.persistence.dataobject.PatientProfileDO;
import me.jianwen.mediask.infra.persistence.mapper.PatientProfileMapper;
import org.springframework.stereotype.Component;

@Component
public class PatientProfileWriteRepositoryAdapter implements PatientProfileWriteRepository {

    private final RedisJsonCacheHelper redisJsonCacheHelper;
    private final PatientProfileMapper patientProfileMapper;

    public PatientProfileWriteRepositoryAdapter(
            RedisJsonCacheHelper redisJsonCacheHelper, PatientProfileMapper patientProfileMapper) {
        this.redisJsonCacheHelper = redisJsonCacheHelper;
        this.patientProfileMapper = patientProfileMapper;
    }

    @Override
    public void updateByUserId(Long userId, PatientProfileDraft draft) {
        PatientProfileDO existingProfile = patientProfileMapper.selectOne(Wrappers.lambdaQuery(PatientProfileDO.class)
                .eq(PatientProfileDO::getUserId, userId)
                .isNull(PatientProfileDO::getDeletedAt));
        if (existingProfile == null) {
            throw new BizException(UserErrorCode.PATIENT_PROFILE_NOT_FOUND);
        }

        PatientProfileDO profileToUpdate = new PatientProfileDO();
        profileToUpdate.setId(existingProfile.getId());
        profileToUpdate.setVersion(existingProfile.getVersion());
        profileToUpdate.setGender(draft.gender());
        profileToUpdate.setBirthDate(draft.birthDate());
        profileToUpdate.setBloodType(draft.bloodType());
        profileToUpdate.setAllergySummary(draft.allergySummary());

        int updatedRows = patientProfileMapper.updateById(profileToUpdate);
        if (updatedRows != 1) {
            throw new BizException(UserErrorCode.PATIENT_PROFILE_UPDATE_CONFLICT);
        }

        // Invalidate after a successful write so a failed update cannot evict a still-valid cache entry.
        redisJsonCacheHelper.delete(CacheKeyGenerator.patientProfileByUserId(userId));
    }
}
