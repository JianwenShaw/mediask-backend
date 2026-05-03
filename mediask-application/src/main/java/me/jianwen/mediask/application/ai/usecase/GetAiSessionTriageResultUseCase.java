package me.jianwen.mediask.application.ai.usecase;

import me.jianwen.mediask.application.audit.AuditResourceTypes;
import me.jianwen.mediask.application.audit.model.AuditContext;
import me.jianwen.mediask.application.ai.query.GetAiSessionTriageResultQuery;
import me.jianwen.mediask.domain.ai.model.AiSessionTriageResult;
import me.jianwen.mediask.domain.ai.model.AiTriageGatewayContext;
import me.jianwen.mediask.domain.ai.port.AiTriageGatewayPort;
import me.jianwen.mediask.domain.audit.model.DataAccessPurposeCode;

public class GetAiSessionTriageResultUseCase {

    private final AiTriageGatewayPort aiTriageGatewayPort;
    private final me.jianwen.mediask.application.audit.usecase.AuditTrailService auditTrailService;

    public GetAiSessionTriageResultUseCase(
            AiTriageGatewayPort aiTriageGatewayPort,
            me.jianwen.mediask.application.audit.usecase.AuditTrailService auditTrailService) {
        this.aiTriageGatewayPort = aiTriageGatewayPort;
        this.auditTrailService = auditTrailService;
    }

    public AiSessionTriageResult handle(
            GetAiSessionTriageResultQuery query, AuditContext auditContext, DataAccessPurposeCode purposeCode) {
        AiSessionTriageResult result = aiTriageGatewayPort.getSessionTriageResult(
                new AiTriageGatewayContext(query.requestId(), query.patientUserId()),
                query.sessionId());
        auditTrailService.recordAllowedDataAccess(
                auditContext,
                AuditResourceTypes.AI_SESSION,
                query.sessionId(),
                query.patientUserId(),
                null,
                purposeCode);
        return result;
    }
}
