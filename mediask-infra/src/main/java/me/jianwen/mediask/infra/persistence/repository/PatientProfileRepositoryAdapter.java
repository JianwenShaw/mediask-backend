package me.jianwen.mediask.infra.persistence.repository;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import java.util.Optional;
import me.jianwen.mediask.common.cache.CacheKeyGenerator;
import me.jianwen.mediask.domain.user.model.PatientProfileSnapshot;
import me.jianwen.mediask.domain.user.port.PatientProfileRepository;
import me.jianwen.mediask.infra.cache.RedisJsonCacheHelper;
import me.jianwen.mediask.infra.cache.UserProfileCachePolicy;
import me.jianwen.mediask.infra.persistence.dataobject.PatientProfileDO;
import me.jianwen.mediask.infra.persistence.mapper.PatientProfileMapper;
import org.springframework.stereotype.Component;

@Component
public class PatientProfileRepositoryAdapter implements PatientProfileRepository {

    private final RedisJsonCacheHelper redisJsonCacheHelper;
    private final PatientProfileMapper patientProfileMapper;

    public PatientProfileRepositoryAdapter(
            RedisJsonCacheHelper redisJsonCacheHelper, PatientProfileMapper patientProfileMapper) {
        this.redisJsonCacheHelper = redisJsonCacheHelper;
        this.patientProfileMapper = patientProfileMapper;
    }

    @Override
    public Optional<PatientProfileSnapshot> findByUserId(Long userId) {
        String cacheKey = CacheKeyGenerator.patientProfileByUserId(userId);
        Optional<PatientProfileSnapshot> cachedProfile = redisJsonCacheHelper.get(cacheKey, PatientProfileSnapshot.class);
        if (cachedProfile.isPresent()) {
            return cachedProfile;
        }

        PatientProfileDO patientProfileDO = patientProfileMapper.selectOne(Wrappers.lambdaQuery(PatientProfileDO.class)
                .eq(PatientProfileDO::getUserId, userId)
                .isNull(PatientProfileDO::getDeletedAt));
        if (patientProfileDO == null) {
            return Optional.empty();
        }
        PatientProfileSnapshot patientProfileSnapshot = new PatientProfileSnapshot(
                patientProfileDO.getId(),
                patientProfileDO.getPatientNo(),
                patientProfileDO.getGender(),
                patientProfileDO.getBirthDate(),
                patientProfileDO.getBloodType(),
                patientProfileDO.getAllergySummary());
        redisJsonCacheHelper.put(cacheKey, patientProfileSnapshot, UserProfileCachePolicy.PATIENT_PROFILE_TTL);
        return Optional.of(patientProfileSnapshot);
    }
}
