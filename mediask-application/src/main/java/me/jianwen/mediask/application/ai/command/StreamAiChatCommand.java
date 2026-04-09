package me.jianwen.mediask.application.ai.command;

import java.util.Objects;
import me.jianwen.mediask.common.util.ArgumentChecks;
import me.jianwen.mediask.domain.ai.model.AiSceneType;

public record StreamAiChatCommand(Long sessionId, String message, Long departmentId, AiSceneType sceneType) {

    public StreamAiChatCommand {
        sessionId = ArgumentChecks.normalizePositive(sessionId, "sessionId");
        message = ArgumentChecks.requireNonBlank(message, "message");
        departmentId = ArgumentChecks.normalizePositive(departmentId, "departmentId");
        sceneType = Objects.requireNonNull(sceneType, "sceneType must not be null");
    }
}
