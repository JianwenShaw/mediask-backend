package me.jianwen.mediask.application.clinical.usecase;

import java.util.List;
import me.jianwen.mediask.application.audit.AuditResourceTypes;
import me.jianwen.mediask.application.audit.model.AuditContext;
import me.jianwen.mediask.application.audit.usecase.AuditTrailService;
import me.jianwen.mediask.application.clinical.query.ListEncounterHistoryEmrsQuery;
import me.jianwen.mediask.common.exception.BizException;
import me.jianwen.mediask.domain.audit.model.DataAccessPurposeCode;
import me.jianwen.mediask.domain.clinical.exception.ClinicalErrorCode;
import me.jianwen.mediask.domain.clinical.model.EmrRecordListItem;
import me.jianwen.mediask.domain.clinical.port.EmrRecordQueryRepository;
import me.jianwen.mediask.domain.clinical.port.EncounterQueryRepository;
import org.springframework.transaction.annotation.Transactional;

public class ListEncounterHistoryEmrsUseCase {

    private final EncounterQueryRepository encounterQueryRepository;
    private final EmrRecordQueryRepository emrRecordQueryRepository;
    private final AuditTrailService auditTrailService;

    public ListEncounterHistoryEmrsUseCase(
            EncounterQueryRepository encounterQueryRepository,
            EmrRecordQueryRepository emrRecordQueryRepository,
            AuditTrailService auditTrailService) {
        this.encounterQueryRepository = encounterQueryRepository;
        this.emrRecordQueryRepository = emrRecordQueryRepository;
        this.auditTrailService = auditTrailService;
    }

    @Transactional(readOnly = true)
    public List<EmrRecordListItem> handle(ListEncounterHistoryEmrsQuery query, AuditContext auditContext) {
        var encounter = encounterQueryRepository.findDetailByEncounterId(query.encounterId())
                .orElseThrow(() -> new BizException(ClinicalErrorCode.EMR_ENCOUNTER_NOT_FOUND));
        Long patientUserId = encounter.patientSummary().patientUserId();
        List<EmrRecordListItem> items =
                emrRecordQueryRepository.listByPatientUserId(patientUserId, query.encounterId());
        auditTrailService.recordAllowedDataAccess(
                auditContext,
                AuditResourceTypes.EMR_SUMMARY,
                String.valueOf(query.encounterId()),
                patientUserId,
                query.encounterId(),
                DataAccessPurposeCode.TREATMENT);
        return items;
    }
}
