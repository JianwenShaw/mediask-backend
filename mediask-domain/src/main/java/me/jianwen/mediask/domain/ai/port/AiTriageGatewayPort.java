package me.jianwen.mediask.domain.ai.port;

import me.jianwen.mediask.domain.ai.model.AiTriageGatewayContext;
import me.jianwen.mediask.domain.ai.model.AiTriageQuery;
import me.jianwen.mediask.domain.ai.model.AiTriageQueryResponse;

public interface AiTriageGatewayPort {

    AiTriageQueryResponse query(AiTriageGatewayContext context, AiTriageQuery query);

    void streamQuery(AiTriageGatewayContext context, AiTriageQuery query, StreamEventHandler handler);

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
