package me.jianwen.mediask.application.ai.usecase;

import me.jianwen.mediask.application.ai.query.GetAiSessionDetailQuery;
import me.jianwen.mediask.domain.ai.model.AiSessionDetail;
import me.jianwen.mediask.domain.ai.model.AiTriageGatewayContext;
import me.jianwen.mediask.domain.ai.port.AiTriageGatewayPort;

public class GetAiSessionDetailUseCase {

    private final AiTriageGatewayPort aiTriageGatewayPort;

    public GetAiSessionDetailUseCase(AiTriageGatewayPort aiTriageGatewayPort) {
        this.aiTriageGatewayPort = aiTriageGatewayPort;
    }

    public AiSessionDetail handle(GetAiSessionDetailQuery query) {
        return aiTriageGatewayPort.getSessionDetail(
                new AiTriageGatewayContext(query.requestId(), query.patientUserId()),
                query.sessionId());
    }
}
