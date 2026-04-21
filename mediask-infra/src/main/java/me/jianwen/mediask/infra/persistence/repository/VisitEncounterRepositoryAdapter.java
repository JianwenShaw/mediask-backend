package me.jianwen.mediask.infra.persistence.repository;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import java.time.OffsetDateTime;
import me.jianwen.mediask.common.id.SnowflakeIdGenerator;
import me.jianwen.mediask.common.request.RequestConstants;
import me.jianwen.mediask.domain.clinical.model.VisitEncounter;
import me.jianwen.mediask.domain.clinical.port.VisitEncounterRepository;
import me.jianwen.mediask.infra.persistence.dataobject.StatusTransitionLogDO;
import me.jianwen.mediask.infra.persistence.dataobject.VisitEncounterDO;
import me.jianwen.mediask.infra.persistence.mapper.StatusTransitionLogMapper;
import me.jianwen.mediask.infra.persistence.mapper.VisitEncounterMapper;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

@Component
public class VisitEncounterRepositoryAdapter implements VisitEncounterRepository {

    private final VisitEncounterMapper visitEncounterMapper;
    private final StatusTransitionLogMapper statusTransitionLogMapper;

    public VisitEncounterRepositoryAdapter(
            VisitEncounterMapper visitEncounterMapper, StatusTransitionLogMapper statusTransitionLogMapper) {
        this.visitEncounterMapper = visitEncounterMapper;
        this.statusTransitionLogMapper = statusTransitionLogMapper;
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
        recordTransition(visitEncounter.encounterId(), null, visitEncounter.status().name(), "CREATE");
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
        if (visitEncounterMapper.updateById(dataObject) == 0) {
            return false;
        }
        recordTransition(existing.getId(), "SCHEDULED", "CANCELLED", "CANCEL_REGISTRATION");
        return true;
    }

    @Override
    public boolean startScheduledByEncounterId(Long encounterId, OffsetDateTime startedAt) {
        if (visitEncounterMapper.startEncounterWhenScheduledAndRegistrationConfirmed(encounterId, startedAt) == 0) {
            return false;
        }
        recordTransition(encounterId, "SCHEDULED", "IN_PROGRESS", "START");
        return true;
    }

    @Override
    public boolean completeInProgressByEncounterId(Long encounterId, OffsetDateTime endedAt) {
        VisitEncounterDO existing = visitEncounterMapper.selectOne(Wrappers.lambdaQuery(VisitEncounterDO.class)
                .eq(VisitEncounterDO::getId, encounterId)
                .isNull(VisitEncounterDO::getDeletedAt));
        if (existing == null || !"IN_PROGRESS".equals(existing.getEncounterStatus())) {
            return false;
        }

        VisitEncounterDO dataObject = new VisitEncounterDO();
        dataObject.setId(existing.getId());
        dataObject.setVersion(existing.getVersion());
        dataObject.setEncounterStatus("COMPLETED");
        dataObject.setEndedAt(endedAt);
        if (visitEncounterMapper.updateById(dataObject) == 0) {
            return false;
        }
        recordTransition(existing.getId(), "IN_PROGRESS", "COMPLETED", "COMPLETE");
        return true;
    }

    private void recordTransition(Long entityId, String fromStatus, String toStatus, String action) {
        StatusTransitionLogDO log = new StatusTransitionLogDO();
        log.setId(SnowflakeIdGenerator.nextId());
        log.setEntityType("VISIT_ENCOUNTER");
        log.setEntityId(entityId);
        log.setFromStatus(fromStatus);
        log.setToStatus(toStatus);
        log.setAction(action);
        log.setOperatorUserId(parseLong(MDC.get(RequestConstants.MDC_USER_ID)));
        log.setRequestId(MDC.get(RequestConstants.MDC_REQUEST_ID));
        log.setOccurredAt(OffsetDateTime.now());
        statusTransitionLogMapper.insert(log);
    }

    private Long parseLong(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return Long.valueOf(value);
    }
}
