package me.jianwen.mediask.application.clinical.usecase;

import java.util.List;
import me.jianwen.mediask.application.audit.AuditResourceTypes;
import me.jianwen.mediask.application.audit.model.AuditContext;
import me.jianwen.mediask.application.audit.usecase.AuditTrailService;
import me.jianwen.mediask.application.clinical.query.ListCurrentPatientEmrsQuery;
import me.jianwen.mediask.domain.audit.model.DataAccessPurposeCode;
import me.jianwen.mediask.domain.clinical.model.EmrRecordListItem;
import me.jianwen.mediask.domain.clinical.port.EmrRecordQueryRepository;
import org.springframework.transaction.annotation.Transactional;

public class ListCurrentPatientEmrsUseCase {

    private final EmrRecordQueryRepository emrRecordQueryRepository;
    private final AuditTrailService auditTrailService;

    public ListCurrentPatientEmrsUseCase(
            EmrRecordQueryRepository emrRecordQueryRepository, AuditTrailService auditTrailService) {
        this.emrRecordQueryRepository = emrRecordQueryRepository;
        this.auditTrailService = auditTrailService;
    }

    @Transactional(readOnly = true)
    public List<EmrRecordListItem> handle(ListCurrentPatientEmrsQuery query, AuditContext auditContext) {
        List<EmrRecordListItem> items = emrRecordQueryRepository.listByPatientUserId(query.patientUserId(), null);
        auditTrailService.recordAllowedDataAccess(
                auditContext,
                AuditResourceTypes.EMR_SUMMARY,
                String.valueOf(query.patientUserId()),
                query.patientUserId(),
                null,
                DataAccessPurposeCode.SELF_SERVICE);
        return items;
    }
}
