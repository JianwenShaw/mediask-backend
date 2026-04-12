package me.jianwen.mediask.domain.ai.port;

import me.jianwen.mediask.domain.ai.model.AiTurn;

public interface AiTurnRepository {

    void save(AiTurn aiTurn);

    int findMaxTurnNoBySessionId(Long sessionId);

    void update(AiTurn aiTurn);
}
