package me.jianwen.mediask.application.ai.usecase;

import me.jianwen.mediask.application.ai.command.SubmitAiTriageQueryCommand;
import me.jianwen.mediask.common.exception.BaseException;
import me.jianwen.mediask.domain.ai.model.AiTriageGatewayContext;
import me.jianwen.mediask.domain.ai.model.AiTriageQuery;
import me.jianwen.mediask.domain.ai.port.AiTriageGatewayPort;

public class StreamAiTriageQueryUseCase {

    private final AiTriageGatewayPort aiTriageGatewayPort;
    private final SubmitAiTriageQueryUseCase submitAiTriageQueryUseCase;

    public StreamAiTriageQueryUseCase(
            AiTriageGatewayPort aiTriageGatewayPort,
            SubmitAiTriageQueryUseCase submitAiTriageQueryUseCase) {
        this.aiTriageGatewayPort = aiTriageGatewayPort;
        this.submitAiTriageQueryUseCase = submitAiTriageQueryUseCase;
    }

    public void handle(SubmitAiTriageQueryCommand command, StreamEventWriter writer) {
        try {
            aiTriageGatewayPort.streamQuery(
                    new AiTriageGatewayContext(command.requestId(), command.patientUserId()),
                    new AiTriageQuery(command.sessionId(), command.hospitalScope(), command.userMessage()),
                    event -> writeEvent(command, writer, event));
        } catch (RuntimeException exception) {
            writer.writeError(errorCode(exception), errorMessage(exception));
        }
    }

    private void writeEvent(SubmitAiTriageQueryCommand command, StreamEventWriter writer, AiTriageGatewayPort.StreamEvent event) {
        if (event.isFinal()) {
            submitAiTriageQueryUseCase.persistIfFinalized(command.hospitalScope(), event.finalResponse());
        }
        writer.writeEvent(event);
    }

    private String errorCode(RuntimeException exception) {
        return exception instanceof BaseException ? "TRIAGE_INTERNAL_ERROR" : "TRIAGE_INTERNAL_ERROR";
    }

    private String errorMessage(RuntimeException exception) {
        String message = exception.getMessage();
        return message == null || message.isBlank() ? "triage stream failed" : message;
    }

    public interface StreamEventWriter {

        void writeEvent(AiTriageGatewayPort.StreamEvent event);

        void writeError(String code, String message);
    }
}
