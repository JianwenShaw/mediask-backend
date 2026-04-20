package me.jianwen.mediask.infra.persistence.repository;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import me.jianwen.mediask.domain.clinical.model.VisitEncounter;
import me.jianwen.mediask.domain.clinical.port.VisitEncounterRepository;
import me.jianwen.mediask.infra.persistence.dataobject.VisitEncounterDO;
import me.jianwen.mediask.infra.persistence.mapper.VisitEncounterMapper;
import org.springframework.stereotype.Component;

@Component
public class VisitEncounterRepositoryAdapter implements VisitEncounterRepository {

    private final VisitEncounterMapper visitEncounterMapper;

    public VisitEncounterRepositoryAdapter(VisitEncounterMapper visitEncounterMapper) {
        this.visitEncounterMapper = visitEncounterMapper;
    }

    @Override
    public void save(VisitEncounter visitEncounter) {
        VisitEncounterDO dataObject = new VisitEncounterDO();
        dataObject.setId(visitEncounter.encounterId());
        dataObject.setOrderId(visitEncounter.registrationId());
        dataObject.setPatientId(visitEncounter.patientUserId());
        dataObject.setDoctorId(visitEncounter.doctorId());
        dataObject.setDepartmentId(visitEncounter.departmentId());
        dataObject.setEncounterStatus(visitEncounter.status().name());
        visitEncounterMapper.insert(dataObject);
    }

    @Override
    public boolean cancelScheduledByRegistrationId(Long registrationId) {
        VisitEncounterDO existing = visitEncounterMapper.selectOne(Wrappers.lambdaQuery(VisitEncounterDO.class)
                .eq(VisitEncounterDO::getOrderId, registrationId)
                .isNull(VisitEncounterDO::getDeletedAt));
        if (existing == null || !"SCHEDULED".equals(existing.getEncounterStatus())) {
            return false;
        }

        VisitEncounterDO dataObject = new VisitEncounterDO();
        dataObject.setId(existing.getId());
        dataObject.setVersion(existing.getVersion());
        dataObject.setEncounterStatus("CANCELLED");
        return visitEncounterMapper.updateById(dataObject) > 0;
    }
}
