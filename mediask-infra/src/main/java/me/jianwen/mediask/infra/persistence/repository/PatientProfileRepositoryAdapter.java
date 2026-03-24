package me.jianwen.mediask.infra.persistence.repository;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import java.util.Optional;
import me.jianwen.mediask.domain.user.model.PatientProfileSnapshot;
import me.jianwen.mediask.domain.user.port.PatientProfileRepository;
import me.jianwen.mediask.infra.persistence.dataobject.PatientProfileDO;
import me.jianwen.mediask.infra.persistence.mapper.PatientProfileMapper;
import org.springframework.stereotype.Component;

@Component
public class PatientProfileRepositoryAdapter implements PatientProfileRepository {

    private final PatientProfileMapper patientProfileMapper;

    public PatientProfileRepositoryAdapter(PatientProfileMapper patientProfileMapper) {
        this.patientProfileMapper = patientProfileMapper;
    }

    @Override
    public Optional<PatientProfileSnapshot> findByUserId(Long userId) {
        PatientProfileDO patientProfileDO = patientProfileMapper.selectOne(Wrappers.lambdaQuery(PatientProfileDO.class)
                .eq(PatientProfileDO::getUserId, userId)
                .isNull(PatientProfileDO::getDeletedAt));
        if (patientProfileDO == null) {
            return Optional.empty();
        }
        return Optional.of(new PatientProfileSnapshot(
                patientProfileDO.getId(),
                patientProfileDO.getPatientNo(),
                patientProfileDO.getGender(),
                patientProfileDO.getBirthDate(),
                patientProfileDO.getBloodType(),
                patientProfileDO.getAllergySummary()));
    }
}
