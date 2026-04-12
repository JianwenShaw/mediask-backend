package me.jianwen.mediask.domain.ai.port;

import java.util.Optional;
import me.jianwen.mediask.domain.ai.model.AiSession;

public interface AiSessionRepository {

    void save(AiSession aiSession);

    Optional<AiSession> findById(Long sessionId);

    void update(AiSession aiSession);
}
