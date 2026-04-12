package me.jianwen.mediask.domain.ai.model;

import me.jianwen.mediask.common.id.SnowflakeIdGenerator;
import me.jianwen.mediask.common.util.ArgumentChecks;

public record AiTurnContent(
        Long id,
        Long turnId,
        AiContentRole role,
        String encryptedContent,
        String maskedContent,
        String contentHash) {

    public AiTurnContent {
        id = ArgumentChecks.requirePositive(id, "id");
        turnId = ArgumentChecks.requirePositive(turnId, "turnId");
        encryptedContent = ArgumentChecks.requireNonBlank(encryptedContent, "encryptedContent");
        maskedContent = ArgumentChecks.blankToNull(maskedContent);
        contentHash = ArgumentChecks.blankToNull(contentHash);
    }

    public static AiTurnContent create(
            Long turnId, AiContentRole role, String encryptedContent, String maskedContent, String contentHash) {
        return new AiTurnContent(
                SnowflakeIdGenerator.nextId(), turnId, role, encryptedContent, maskedContent, contentHash);
    }
}
