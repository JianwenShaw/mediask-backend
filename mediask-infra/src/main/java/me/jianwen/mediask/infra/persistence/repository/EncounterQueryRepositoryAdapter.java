package me.jianwen.mediask.infra.persistence.repository;

import java.util.List;
import java.util.Optional;
import me.jianwen.mediask.domain.clinical.model.EncounterDetail;
import me.jianwen.mediask.domain.clinical.model.EncounterListItem;
import me.jianwen.mediask.domain.clinical.model.EncounterPatientSummary;
import me.jianwen.mediask.domain.clinical.model.VisitEncounterStatus;
import me.jianwen.mediask.domain.clinical.port.EncounterQueryRepository;
import me.jianwen.mediask.infra.persistence.mapper.VisitEncounterDetailRow;
import me.jianwen.mediask.infra.persistence.mapper.VisitEncounterListRow;
import me.jianwen.mediask.infra.persistence.mapper.VisitEncounterMapper;
import org.springframework.stereotype.Component;

@Component
public class EncounterQueryRepositoryAdapter implements EncounterQueryRepository {

    private final VisitEncounterMapper visitEncounterMapper;

    public EncounterQueryRepositoryAdapter(VisitEncounterMapper visitEncounterMapper) {
        this.visitEncounterMapper = visitEncounterMapper;
    }

    @Override
    public List<EncounterListItem> listByDoctorId(Long doctorId, VisitEncounterStatus status) {
        return visitEncounterMapper
                .selectDoctorEncounters(doctorId, status == null ? null : status.name())
                .stream()
                .map(this::toListItem)
                .toList();
    }

    @Override
    public Optional<EncounterDetail> findDetailByEncounterId(Long encounterId) {
        VisitEncounterDetailRow row = visitEncounterMapper.selectEncounterDetail(encounterId);
        return Optional.ofNullable(row).map(this::toDetail);
    }

    private EncounterListItem toListItem(VisitEncounterListRow row) {
        return new EncounterListItem(
                row.getEncounterId(),
                row.getRegistrationId(),
                row.getPatientUserId(),
                row.getPatientName(),
                row.getDepartmentId(),
                row.getDepartmentName(),
                row.getSessionDate(),
                row.getPeriodCode(),
                VisitEncounterStatus.valueOf(row.getEncounterStatus()),
                row.getStartedAt(),
                row.getEndedAt());
    }

    private EncounterDetail toDetail(VisitEncounterDetailRow row) {
        return new EncounterDetail(
                row.getEncounterId(),
                row.getRegistrationId(),
                row.getDoctorId(),
                new EncounterPatientSummary(
                        row.getPatientUserId(),
                        row.getPatientName(),
                        row.getGender(),
                        row.getDepartmentId(),
                        row.getDepartmentName(),
                        row.getSessionDate(),
                        row.getPeriodCode(),
                        VisitEncounterStatus.valueOf(row.getEncounterStatus()),
                row.getStartedAt(),
                row.getEndedAt(),
                row.getBirthDate()));
    }
}
