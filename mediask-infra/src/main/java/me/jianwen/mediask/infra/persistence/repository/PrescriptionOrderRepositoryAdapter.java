package me.jianwen.mediask.infra.persistence.repository;

import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import me.jianwen.mediask.common.exception.BizException;
import me.jianwen.mediask.domain.clinical.exception.ClinicalErrorCode;
import me.jianwen.mediask.domain.clinical.model.PrescriptionItem;
import me.jianwen.mediask.domain.clinical.model.PrescriptionOrder;
import me.jianwen.mediask.domain.clinical.model.PrescriptionStatus;
import me.jianwen.mediask.domain.clinical.port.PrescriptionOrderQueryRepository;
import me.jianwen.mediask.domain.clinical.port.PrescriptionOrderRepository;
import me.jianwen.mediask.infra.persistence.dataobject.PrescriptionItemDO;
import me.jianwen.mediask.infra.persistence.dataobject.PrescriptionOrderDO;
import me.jianwen.mediask.infra.persistence.mapper.PrescriptionOrderMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;

@Component
public class PrescriptionOrderRepositoryAdapter implements PrescriptionOrderRepository, PrescriptionOrderQueryRepository {

    private final PrescriptionOrderMapper prescriptionOrderMapper;

    public PrescriptionOrderRepositoryAdapter(PrescriptionOrderMapper prescriptionOrderMapper) {
        this.prescriptionOrderMapper = prescriptionOrderMapper;
    }

    @Override
    public void save(PrescriptionOrder prescriptionOrder) {
        try {
            PrescriptionOrderDO orderDO = new PrescriptionOrderDO();
            orderDO.setId(prescriptionOrder.prescriptionOrderId());
            orderDO.setPrescriptionNo(prescriptionOrder.prescriptionNo());
            orderDO.setRecordId(prescriptionOrder.recordId());
            orderDO.setEncounterId(prescriptionOrder.encounterId());
            orderDO.setPatientId(prescriptionOrder.patientId());
            orderDO.setDoctorId(prescriptionOrder.doctorId());
            orderDO.setPrescriptionStatus(prescriptionOrder.prescriptionStatus().name());
            orderDO.setVersion(prescriptionOrder.version());
            orderDO.setCreatedAt(prescriptionOrder.createdAt().atOffset(ZoneOffset.UTC));
            orderDO.setUpdatedAt(prescriptionOrder.updatedAt().atOffset(ZoneOffset.UTC));
            prescriptionOrderMapper.insert(orderDO);

            List<PrescriptionItemDO> itemDOs = prescriptionOrder.items().stream()
                    .map(item -> {
                        PrescriptionItemDO itemDO = new PrescriptionItemDO();
                        itemDO.setId(item.itemId());
                        itemDO.setPrescriptionId(prescriptionOrder.prescriptionOrderId());
                        itemDO.setSortOrder(item.sortOrder());
                        itemDO.setDrugName(item.drugName());
                        itemDO.setDrugSpecification(item.drugSpecification());
                        itemDO.setDosageText(item.dosageText());
                        itemDO.setFrequencyText(item.frequencyText());
                        itemDO.setDurationText(item.durationText());
                        itemDO.setQuantity(item.quantity());
                        itemDO.setUnit(item.unit());
                        itemDO.setRoute(item.route());
                        itemDO.setCreatedAt(prescriptionOrder.createdAt().atOffset(ZoneOffset.UTC));
                        return itemDO;
                    })
                    .toList();
            prescriptionOrderMapper.insertItems(itemDOs);
        } catch (DuplicateKeyException exception) {
            throw mapDuplicateKeyOnSave(exception);
        }
    }

    @Override
    public boolean existsByEncounterId(Long encounterId) {
        return prescriptionOrderMapper.existsByEncounterId(encounterId);
    }

    @Override
    public Optional<PrescriptionOrder> findByEncounterId(Long encounterId) {
        return prescriptionOrderMapper.selectByEncounterId(encounterId)
                .map(orderDO -> new PrescriptionOrder(
                        orderDO.getId(),
                        orderDO.getPrescriptionNo(),
                        orderDO.getRecordId(),
                        orderDO.getEncounterId(),
                        orderDO.getPatientId(),
                        orderDO.getDoctorId(),
                        PrescriptionStatus.valueOf(orderDO.getPrescriptionStatus()),
                        prescriptionOrderMapper.selectItemsByPrescriptionId(orderDO.getId()).stream()
                                .map(itemDO -> new PrescriptionItem(
                                        itemDO.getId(),
                                        itemDO.getSortOrder(),
                                        itemDO.getDrugName(),
                                        itemDO.getDrugSpecification(),
                                        itemDO.getDosageText(),
                                        itemDO.getFrequencyText(),
                                        itemDO.getDurationText(),
                                        itemDO.getQuantity(),
                                        itemDO.getUnit(),
                                        itemDO.getRoute()))
                                .toList(),
                        orderDO.getVersion(),
                        orderDO.getCreatedAt().toInstant(),
                        orderDO.getUpdatedAt().toInstant()));
    }

    private BizException mapDuplicateKeyOnSave(DuplicateKeyException exception) {
        String message = exception.getMessage();
        if (message != null && message.contains("uk_prescription_order_encounter")) {
            return new BizException(ClinicalErrorCode.PRESCRIPTION_ALREADY_EXISTS);
        }
        throw exception;
    }
}
