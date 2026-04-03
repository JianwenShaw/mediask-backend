package me.jianwen.mediask.infra.persistence.repository;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import java.util.List;
import me.jianwen.mediask.domain.outpatient.model.RegistrationListItem;
import me.jianwen.mediask.domain.outpatient.model.RegistrationStatus;
import me.jianwen.mediask.domain.outpatient.port.RegistrationOrderQueryRepository;
import me.jianwen.mediask.infra.persistence.dataobject.RegistrationOrderDO;
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
        return registrationOrderMapper.selectList(Wrappers.lambdaQuery(RegistrationOrderDO.class)
                        .eq(RegistrationOrderDO::getPatientId, patientUserId)
                        .eq(status != null, RegistrationOrderDO::getOrderStatus, status.name())
                        .isNull(RegistrationOrderDO::getDeletedAt)
                        .orderByDesc(RegistrationOrderDO::getCreatedAt))
                .stream()
                .map(this::toListItem)
                .toList();
    }

    private RegistrationListItem toListItem(RegistrationOrderDO dataObject) {
        return new RegistrationListItem(
                dataObject.getId(),
                dataObject.getOrderNo(),
                RegistrationStatus.valueOf(dataObject.getOrderStatus()),
                dataObject.getCreatedAt(),
                dataObject.getSourceAiSessionId());
    }
}
