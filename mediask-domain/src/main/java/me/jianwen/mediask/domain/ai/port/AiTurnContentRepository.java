package me.jianwen.mediask.domain.ai.port;

import me.jianwen.mediask.domain.ai.model.AiTurnContent;

public interface AiTurnContentRepository {

    void save(AiTurnContent aiTurnContent);
}
