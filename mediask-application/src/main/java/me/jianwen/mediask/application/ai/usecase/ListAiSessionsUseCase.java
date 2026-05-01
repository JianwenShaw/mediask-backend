package me.jianwen.mediask.application.ai.usecase;

import me.jianwen.mediask.application.ai.query.ListAiSessionsQuery;
import me.jianwen.mediask.domain.ai.model.AiSessionSummaryList;
import me.jianwen.mediask.domain.ai.model.AiTriageGatewayContext;
import me.jianwen.mediask.domain.ai.port.AiTriageGatewayPort;

public class ListAiSessionsUseCase {

    private final AiTriageGatewayPort aiTriageGatewayPort;

    public ListAiSessionsUseCase(AiTriageGatewayPort aiTriageGatewayPort) {
        this.aiTriageGatewayPort = aiTriageGatewayPort;
    }

    public AiSessionSummaryList handle(ListAiSessionsQuery query) {
        return aiTriageGatewayPort.listSessions(new AiTriageGatewayContext(query.requestId(), query.patientUserId()));
    }
}
