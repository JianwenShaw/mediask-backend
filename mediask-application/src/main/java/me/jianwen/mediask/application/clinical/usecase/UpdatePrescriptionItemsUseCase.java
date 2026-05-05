package me.jianwen.mediask.application.clinical.usecase;

import java.util.List;
import me.jianwen.mediask.application.audit.AuditActionCodes;
import me.jianwen.mediask.application.audit.AuditResourceTypes;
import me.jianwen.mediask.application.audit.model.AuditContext;
import me.jianwen.mediask.application.clinical.command.UpdatePrescriptionItemsCommand;
import me.jianwen.mediask.common.exception.BizException;
import me.jianwen.mediask.common.id.SnowflakeIdGenerator;
import me.jianwen.mediask.domain.clinical.exception.ClinicalErrorCode;
import me.jianwen.mediask.domain.clinical.model.PrescriptionItem;
import me.jianwen.mediask.domain.clinical.model.PrescriptionOrder;
import me.jianwen.mediask.domain.clinical.port.EncounterQueryRepository;
import me.jianwen.mediask.domain.clinical.port.PrescriptionOrderQueryRepository;
import me.jianwen.mediask.domain.clinical.port.PrescriptionOrderRepository;
import org.springframework.transaction.annotation.Transactional;

public class UpdatePrescriptionItemsUseCase {

    private final PrescriptionOrderQueryRepository prescriptionOrderQueryRepository;
    private final PrescriptionOrderRepository prescriptionOrderRepository;
    private final EncounterQueryRepository encounterQueryRepository;
    private final me.jianwen.mediask.application.audit.usecase.AuditTrailService auditTrailService;

    public UpdatePrescriptionItemsUseCase(
            PrescriptionOrderQueryRepository prescriptionOrderQueryRepository,
            PrescriptionOrderRepository prescriptionOrderRepository,
            EncounterQueryRepository encounterQueryRepository,
            me.jianwen.mediask.application.audit.usecase.AuditTrailService auditTrailService) {
        this.prescriptionOrderQueryRepository = prescriptionOrderQueryRepository;
        this.prescriptionOrderRepository = prescriptionOrderRepository;
        this.encounterQueryRepository = encounterQueryRepository;
        this.auditTrailService = auditTrailService;
    }

    @Transactional
    public PrescriptionOrder handle(UpdatePrescriptionItemsCommand command, AuditContext auditContext) {
        var encounter = encounterQueryRepository.findDetailByEncounterId(command.encounterId())
                .orElseThrow(() -> new BizException(ClinicalErrorCode.PRESCRIPTION_ENCOUNTER_NOT_FOUND));
        if (!encounter.doctorId().equals(command.doctorId())) {
            throw new BizException(ClinicalErrorCode.PRESCRIPTION_ENCOUNTER_NOT_FOUND);
        }

        PrescriptionOrder current = prescriptionOrderQueryRepository.findByEncounterId(command.encounterId())
                .orElseThrow(() -> new BizException(ClinicalErrorCode.PRESCRIPTION_NOT_FOUND));

        List<PrescriptionItem> newItems = command.items().stream()
                .map(item -> new PrescriptionItem(
                        SnowflakeIdGenerator.nextId(),
                        item.sortOrder(),
                        item.drugName(),
                        item.drugSpecification(),
                        item.dosageText(),
                        item.frequencyText(),
                        item.durationText(),
                        item.quantity(),
                        item.unit(),
                        item.route()))
                .toList();

        PrescriptionOrder updated;
        try {
            updated = current.updateItems(newItems);
        } catch (IllegalStateException e) {
            throw new BizException(ClinicalErrorCode.PRESCRIPTION_STATUS_TRANSITION_NOT_ALLOWED);
        }

        if (!prescriptionOrderRepository.update(updated)) {
            throw new BizException(ClinicalErrorCode.PRESCRIPTION_UPDATE_CONFLICT);
        }

        auditTrailService.recordAuditSuccess(
                auditContext,
                AuditActionCodes.PRESCRIPTION_UPDATE,
                AuditResourceTypes.PRESCRIPTION_ORDER,
                String.valueOf(updated.prescriptionOrderId()),
                updated.patientId(),
                updated.encounterId(),
                null);
        return updated;
    }
}
