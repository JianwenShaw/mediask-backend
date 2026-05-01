package me.jianwen.mediask.application.ai.usecase;

import me.jianwen.mediask.application.ai.query.GetAiSessionTriageResultQuery;
import me.jianwen.mediask.domain.ai.model.AiSessionTriageResult;
import me.jianwen.mediask.domain.ai.model.AiTriageGatewayContext;
import me.jianwen.mediask.domain.ai.port.AiTriageGatewayPort;

public class GetAiSessionTriageResultUseCase {

    private final AiTriageGatewayPort aiTriageGatewayPort;

    public GetAiSessionTriageResultUseCase(AiTriageGatewayPort aiTriageGatewayPort) {
        this.aiTriageGatewayPort = aiTriageGatewayPort;
    }

    public AiSessionTriageResult handle(GetAiSessionTriageResultQuery query) {
        return aiTriageGatewayPort.getSessionTriageResult(
                new AiTriageGatewayContext(query.requestId(), query.patientUserId()),
                query.sessionId());
    }
}
