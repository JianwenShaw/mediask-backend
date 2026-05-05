package me.jianwen.mediask.infra.persistence.repository;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import java.util.List;
import java.util.Optional;
import me.jianwen.mediask.domain.outpatient.model.ClinicSessionPeriodCode;
import me.jianwen.mediask.domain.outpatient.model.RegistrationDetail;
import me.jianwen.mediask.domain.outpatient.model.RegistrationListItem;
import me.jianwen.mediask.domain.outpatient.model.RegistrationStatus;
import me.jianwen.mediask.domain.outpatient.port.RegistrationOrderQueryRepository;
import me.jianwen.mediask.infra.persistence.dataobject.RegistrationOrderDO;
import me.jianwen.mediask.infra.persistence.mapper.RegistrationDetailRow;
import me.jianwen.mediask.infra.persistence.mapper.RegistrationOrderMapper;
import org.springframework.stereotype.Component;

@Component
public class RegistrationOrderQueryRepositoryAdapter implements RegistrationOrderQueryRepository {

    private final RegistrationOrderMapper registrationOrderMapper;

    public RegistrationOrderQueryRepositoryAdapter(RegistrationOrderMapper registrationOrderMapper) {
        this.registrationOrderMapper = registrationOrderMapper;
    }

    @Override
    public List<RegistrationListItem> listByPatientUserId(Long patientUserId, RegistrationStatus status) {
        var query = Wrappers.lambdaQuery(RegistrationOrderDO.class)
                .eq(RegistrationOrderDO::getPatientId, patientUserId)
                .isNull(RegistrationOrderDO::getDeletedAt)
                .orderByDesc(RegistrationOrderDO::getCreatedAt);
        if (status != null) {
            query.eq(RegistrationOrderDO::getOrderStatus, status.name());
        }
        return registrationOrderMapper.selectList(query)
                .stream()
                .map(this::toListItem)
                .toList();
    }

    @Override
    public Optional<RegistrationDetail> findDetailByPatientUserIdAndRegistrationId(Long patientUserId, Long registrationId) {
        return Optional.ofNullable(registrationOrderMapper.selectRegistrationDetail(patientUserId, registrationId))
                .map(this::toDetail);
    }

    @Override
    public Optional<String> findSourceAiSessionIdByRegistrationId(Long registrationId) {
        return Optional.ofNullable(registrationOrderMapper.selectOne(Wrappers.lambdaQuery(RegistrationOrderDO.class)
                        .eq(RegistrationOrderDO::getId, registrationId)
                        .isNull(RegistrationOrderDO::getDeletedAt)))
                .map(RegistrationOrderDO::getSourceAiSessionId)
                .filter(sourceAiSessionId -> !sourceAiSessionId.isBlank());
    }

    private RegistrationListItem toListItem(RegistrationOrderDO dataObject) {
        return new RegistrationListItem(
                dataObject.getId(),
                dataObject.getOrderNo(),
                RegistrationStatus.valueOf(dataObject.getOrderStatus()),
                dataObject.getCreatedAt(),
                dataObject.getSourceAiSessionId());
    }

    private RegistrationDetail toDetail(RegistrationDetailRow row) {
        return new RegistrationDetail(
                row.getRegistrationId(),
                row.getPatientUserId(),
                row.getOrderNo(),
                RegistrationStatus.valueOf(row.getOrderStatus()),
                row.getCreatedAt(),
                row.getSourceAiSessionId(),
                row.getClinicSessionId(),
                row.getClinicSlotId(),
                row.getDepartmentId(),
                row.getDepartmentName(),
                row.getDoctorId(),
                row.getDoctorName(),
                row.getSessionDate(),
                row.getPeriodCode() == null ? null : ClinicSessionPeriodCode.valueOf(row.getPeriodCode()),
                row.getFee(),
                row.getCancelledAt(),
                row.getCancellationReason());
    }
}
