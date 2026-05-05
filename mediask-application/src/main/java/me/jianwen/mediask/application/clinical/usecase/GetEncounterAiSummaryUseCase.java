package me.jianwen.mediask.application.clinical.usecase;

import me.jianwen.mediask.application.audit.AuditResourceTypes;
import me.jianwen.mediask.application.audit.model.AuditContext;
import me.jianwen.mediask.application.audit.usecase.AuditTrailService;
import me.jianwen.mediask.application.clinical.query.GetEncounterAiSummaryQuery;
import me.jianwen.mediask.common.exception.BizException;
import me.jianwen.mediask.common.exception.ErrorCode;
import me.jianwen.mediask.domain.ai.exception.AiErrorCode;
import me.jianwen.mediask.domain.ai.model.AiSessionTriageResult;
import me.jianwen.mediask.domain.ai.model.AiTriageGatewayContext;
import me.jianwen.mediask.domain.ai.port.AiTriageGatewayPort;
import me.jianwen.mediask.domain.audit.model.DataAccessPurposeCode;
import me.jianwen.mediask.domain.clinical.exception.ClinicalErrorCode;
import me.jianwen.mediask.domain.clinical.model.EncounterAiSummary;
import me.jianwen.mediask.domain.clinical.model.EncounterDetail;
import me.jianwen.mediask.domain.clinical.port.EncounterQueryRepository;
import me.jianwen.mediask.domain.outpatient.port.RegistrationOrderQueryRepository;
import org.springframework.transaction.annotation.Transactional;

public class GetEncounterAiSummaryUseCase {

    private final EncounterQueryRepository encounterQueryRepository;
    private final RegistrationOrderQueryRepository registrationOrderQueryRepository;
    private final AiTriageGatewayPort aiTriageGatewayPort;
    private final AuditTrailService auditTrailService;

    public GetEncounterAiSummaryUseCase(
            EncounterQueryRepository encounterQueryRepository,
            RegistrationOrderQueryRepository registrationOrderQueryRepository,
            AiTriageGatewayPort aiTriageGatewayPort,
            AuditTrailService auditTrailService) {
        this.encounterQueryRepository = encounterQueryRepository;
        this.registrationOrderQueryRepository = registrationOrderQueryRepository;
        this.aiTriageGatewayPort = aiTriageGatewayPort;
        this.auditTrailService = auditTrailService;
    }

    @Transactional(readOnly = true)
    public EncounterAiSummary handle(GetEncounterAiSummaryQuery query, AuditContext auditContext) {
        EncounterDetail detail = encounterQueryRepository
                .findDetailByEncounterId(query.encounterId())
                .orElseThrow(() -> new BizException(ClinicalErrorCode.ENCOUNTER_NOT_FOUND));
        if (!detail.doctorId().equals(query.doctorId())) {
            throw new BizException(ClinicalErrorCode.ENCOUNTER_ACCESS_DENIED);
        }
        String sourceAiSessionId = registrationOrderQueryRepository
                .findSourceAiSessionIdByRegistrationId(detail.registrationId())
                .orElseThrow(() -> new BizException(ClinicalErrorCode.ENCOUNTER_AI_SUMMARY_NOT_FOUND));

        AiSessionTriageResult triageResult;
        try {
            triageResult = aiTriageGatewayPort.getSessionTriageResult(
                    new AiTriageGatewayContext(auditContext.requestId(), detail.patientSummary().patientUserId()),
                    sourceAiSessionId);
        } catch (BizException exception) {
            if (exception.getErrorCode() == ErrorCode.RESOURCE_NOT_FOUND
                    || exception.getErrorCode() == AiErrorCode.TRIAGE_RESULT_NOT_READY) {
                throw new BizException(ClinicalErrorCode.ENCOUNTER_AI_SUMMARY_NOT_FOUND);
            }
            throw exception;
        }

        auditTrailService.recordAllowedDataAccess(
                auditContext,
                AuditResourceTypes.AI_SESSION,
                sourceAiSessionId,
                detail.patientSummary().patientUserId(),
                detail.encounterId(),
                DataAccessPurposeCode.TREATMENT);
        return new EncounterAiSummary(
                detail.encounterId(),
                detail.patientSummary().patientUserId(),
                triageResult.sessionId(),
                triageResult.chiefComplaintSummary(),
                triageResult.riskLevel(),
                triageResult.recommendedDepartments(),
                triageResult.careAdvice(),
                triageResult.citations(),
                triageResult.blockedReason(),
                triageResult.catalogVersion(),
                triageResult.finalizedAt());
    }
}
