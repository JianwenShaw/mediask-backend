package me.jianwen.mediask.application.clinical.usecase;

import me.jianwen.mediask.application.audit.AuditResourceTypes;
import me.jianwen.mediask.application.audit.model.AuditContext;
import me.jianwen.mediask.application.clinical.query.GetPrescriptionDetailQuery;
import me.jianwen.mediask.common.exception.BizException;
import me.jianwen.mediask.domain.clinical.exception.ClinicalErrorCode;
import me.jianwen.mediask.domain.clinical.model.PrescriptionOrder;
import me.jianwen.mediask.domain.clinical.port.EncounterQueryRepository;
import me.jianwen.mediask.domain.clinical.port.PrescriptionOrderQueryRepository;
import me.jianwen.mediask.domain.audit.model.DataAccessPurposeCode;

public class GetPrescriptionDetailUseCase {

    private final PrescriptionOrderQueryRepository prescriptionOrderQueryRepository;
    private final EncounterQueryRepository encounterQueryRepository;
    private final me.jianwen.mediask.application.audit.usecase.AuditTrailService auditTrailService;

    public GetPrescriptionDetailUseCase(
            PrescriptionOrderQueryRepository prescriptionOrderQueryRepository,
            EncounterQueryRepository encounterQueryRepository,
            me.jianwen.mediask.application.audit.usecase.AuditTrailService auditTrailService) {
        this.prescriptionOrderQueryRepository = prescriptionOrderQueryRepository;
        this.encounterQueryRepository = encounterQueryRepository;
        this.auditTrailService = auditTrailService;
    }

    public PrescriptionOrder handle(
            GetPrescriptionDetailQuery query, AuditContext auditContext, DataAccessPurposeCode purposeCode) {
        var encounter = encounterQueryRepository.findDetailByEncounterId(query.encounterId())
                .orElseThrow(() -> new BizException(ClinicalErrorCode.PRESCRIPTION_ENCOUNTER_NOT_FOUND));
        if (query.doctorId() != null && encounter.doctorId().equals(query.doctorId())) {
            return readAndAudit(query, auditContext, purposeCode);
        }
        if (query.patientUserId() != null && encounter.patientSummary().patientUserId().equals(query.patientUserId())) {
            return readAndAudit(query, auditContext, purposeCode);
        }
        if (query.doctorId() != null || query.patientUserId() != null) {
            throw new BizException(ClinicalErrorCode.PRESCRIPTION_ENCOUNTER_NOT_FOUND);
        }
        throw new BizException(ClinicalErrorCode.PRESCRIPTION_ENCOUNTER_NOT_FOUND);
    }

    private PrescriptionOrder readAndAudit(
            GetPrescriptionDetailQuery query, AuditContext auditContext, DataAccessPurposeCode purposeCode) {
        PrescriptionOrder order = prescriptionOrderQueryRepository.findByEncounterId(query.encounterId())
                .orElseThrow(() -> new BizException(ClinicalErrorCode.PRESCRIPTION_NOT_FOUND));
        auditTrailService.recordAllowedDataAccess(
                auditContext,
                AuditResourceTypes.PRESCRIPTION_DETAIL,
                String.valueOf(query.encounterId()),
                order.patientId(),
                order.encounterId(),
                purposeCode);
        return order;
    }
}
