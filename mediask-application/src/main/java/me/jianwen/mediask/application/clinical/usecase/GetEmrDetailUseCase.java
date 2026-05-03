package me.jianwen.mediask.application.clinical.usecase;

import me.jianwen.mediask.application.audit.AuditResourceTypes;
import me.jianwen.mediask.application.audit.model.AuditContext;
import me.jianwen.mediask.application.clinical.query.GetEmrDetailQuery;
import me.jianwen.mediask.common.exception.BizException;
import me.jianwen.mediask.domain.clinical.exception.ClinicalErrorCode;
import me.jianwen.mediask.domain.clinical.model.EmrRecord;
import me.jianwen.mediask.domain.clinical.port.EmrRecordQueryRepository;
import me.jianwen.mediask.domain.audit.model.DataAccessPurposeCode;
import org.springframework.transaction.annotation.Transactional;

public class GetEmrDetailUseCase {

    private final EmrRecordQueryRepository emrRecordQueryRepository;
    private final me.jianwen.mediask.application.audit.usecase.AuditTrailService auditTrailService;

    public GetEmrDetailUseCase(
            EmrRecordQueryRepository emrRecordQueryRepository,
            me.jianwen.mediask.application.audit.usecase.AuditTrailService auditTrailService) {
        this.emrRecordQueryRepository = emrRecordQueryRepository;
        this.auditTrailService = auditTrailService;
    }

    @Transactional(readOnly = true)
    public EmrRecord handle(GetEmrDetailQuery query, AuditContext auditContext, DataAccessPurposeCode purposeCode) {
        EmrRecord record = emrRecordQueryRepository
                .findByEncounterId(query.encounterId())
                .orElseThrow(() -> new BizException(ClinicalErrorCode.EMR_RECORD_NOT_FOUND));
        auditTrailService.recordAllowedDataAccess(
                auditContext,
                AuditResourceTypes.EMR_CONTENT,
                String.valueOf(query.encounterId()),
                record.patientId(),
                record.encounterId(),
                purposeCode);
        return record;
    }
}
