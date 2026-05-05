package me.jianwen.mediask.application.clinical.usecase;

import me.jianwen.mediask.application.audit.AuditActionCodes;
import me.jianwen.mediask.application.audit.AuditResourceTypes;
import me.jianwen.mediask.application.audit.model.AuditContext;
import me.jianwen.mediask.application.clinical.command.IssuePrescriptionCommand;
import me.jianwen.mediask.common.exception.BizException;
import me.jianwen.mediask.domain.clinical.exception.ClinicalErrorCode;
import me.jianwen.mediask.domain.clinical.model.PrescriptionOrder;
import me.jianwen.mediask.domain.clinical.port.EncounterQueryRepository;
import me.jianwen.mediask.domain.clinical.port.PrescriptionOrderQueryRepository;
import me.jianwen.mediask.domain.clinical.port.PrescriptionOrderRepository;
import org.springframework.transaction.annotation.Transactional;

public class IssuePrescriptionUseCase {

    private final PrescriptionOrderQueryRepository prescriptionOrderQueryRepository;
    private final PrescriptionOrderRepository prescriptionOrderRepository;
    private final EncounterQueryRepository encounterQueryRepository;
    private final me.jianwen.mediask.application.audit.usecase.AuditTrailService auditTrailService;

    public IssuePrescriptionUseCase(
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
    public PrescriptionOrder handle(IssuePrescriptionCommand command, AuditContext auditContext) {
        var encounter = encounterQueryRepository.findDetailByEncounterId(command.encounterId())
                .orElseThrow(() -> new BizException(ClinicalErrorCode.PRESCRIPTION_ENCOUNTER_NOT_FOUND));
        if (!encounter.doctorId().equals(command.doctorId())) {
            throw new BizException(ClinicalErrorCode.PRESCRIPTION_ENCOUNTER_NOT_FOUND);
        }

        PrescriptionOrder current = prescriptionOrderQueryRepository.findByEncounterId(command.encounterId())
                .orElseThrow(() -> new BizException(ClinicalErrorCode.PRESCRIPTION_NOT_FOUND));

        PrescriptionOrder issued;
        try {
            issued = current.issue();
        } catch (IllegalStateException e) {
            throw new BizException(ClinicalErrorCode.PRESCRIPTION_STATUS_TRANSITION_NOT_ALLOWED);
        }

        if (!prescriptionOrderRepository.update(issued)) {
            throw new BizException(ClinicalErrorCode.PRESCRIPTION_UPDATE_CONFLICT);
        }

        auditTrailService.recordAuditSuccess(
                auditContext,
                AuditActionCodes.PRESCRIPTION_ISSUE,
                AuditResourceTypes.PRESCRIPTION_ORDER,
                String.valueOf(issued.prescriptionOrderId()),
                issued.patientId(),
                issued.encounterId(),
                null);
        return issued;
    }
}
