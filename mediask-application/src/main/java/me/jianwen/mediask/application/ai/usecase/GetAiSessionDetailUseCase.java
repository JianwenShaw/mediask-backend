package me.jianwen.mediask.application.ai.usecase;

import me.jianwen.mediask.application.audit.AuditResourceTypes;
import me.jianwen.mediask.application.audit.model.AuditContext;
import me.jianwen.mediask.application.ai.query.GetAiSessionDetailQuery;
import me.jianwen.mediask.domain.ai.model.AiSessionDetail;
import me.jianwen.mediask.domain.ai.model.AiTriageGatewayContext;
import me.jianwen.mediask.domain.ai.port.AiTriageGatewayPort;
import me.jianwen.mediask.domain.audit.model.DataAccessPurposeCode;

public class GetAiSessionDetailUseCase {

    private final AiTriageGatewayPort aiTriageGatewayPort;
    private final me.jianwen.mediask.application.audit.usecase.AuditTrailService auditTrailService;

    public GetAiSessionDetailUseCase(
            AiTriageGatewayPort aiTriageGatewayPort,
            me.jianwen.mediask.application.audit.usecase.AuditTrailService auditTrailService) {
        this.aiTriageGatewayPort = aiTriageGatewayPort;
        this.auditTrailService = auditTrailService;
    }

    public AiSessionDetail handle(
            GetAiSessionDetailQuery query, AuditContext auditContext, DataAccessPurposeCode purposeCode) {
        AiSessionDetail detail = aiTriageGatewayPort.getSessionDetail(
                new AiTriageGatewayContext(query.requestId(), query.patientUserId()),
                query.sessionId());
        auditTrailService.recordAllowedDataAccess(
                auditContext,
                AuditResourceTypes.AI_SESSION,
                query.sessionId(),
                query.patientUserId(),
                null,
                purposeCode);
        return detail;
    }
}
