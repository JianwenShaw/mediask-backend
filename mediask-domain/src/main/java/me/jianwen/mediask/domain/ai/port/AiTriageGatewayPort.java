package me.jianwen.mediask.domain.ai.port;

import me.jianwen.mediask.domain.ai.model.AiTriageGatewayContext;
import me.jianwen.mediask.domain.ai.model.AiTriageQuery;
import me.jianwen.mediask.domain.ai.model.AiTriageQueryResponse;
import me.jianwen.mediask.domain.ai.model.AiSessionDetail;
import me.jianwen.mediask.domain.ai.model.AiSessionSummaryList;
import me.jianwen.mediask.domain.ai.model.AiSessionTriageResult;

public interface AiTriageGatewayPort {

    AiTriageQueryResponse query(AiTriageGatewayContext context, AiTriageQuery query);

    void streamQuery(AiTriageGatewayContext context, AiTriageQuery query, StreamEventHandler handler);

    AiSessionSummaryList listSessions(AiTriageGatewayContext context);

    AiSessionDetail getSessionDetail(AiTriageGatewayContext context, String sessionId);

    AiSessionTriageResult getSessionTriageResult(AiTriageGatewayContext context, String sessionId);

    @FunctionalInterface
    interface StreamEventHandler {
        void onEvent(StreamEvent event);
    }

    record StreamEvent(String event, String data, AiTriageQueryResponse finalResponse) {

        public StreamEvent {
            if ("final".equals(event) && finalResponse == null) {
                throw new IllegalArgumentException("finalResponse is required for final event");
            }
        }

        public boolean isFinal() {
            return "final".equals(event);
        }
    }
}
