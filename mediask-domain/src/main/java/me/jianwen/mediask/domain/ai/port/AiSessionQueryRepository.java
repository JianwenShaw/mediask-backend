package me.jianwen.mediask.domain.ai.port;

import java.util.Optional;
import me.jianwen.mediask.domain.ai.model.AiSessionDetail;
import me.jianwen.mediask.domain.ai.model.AiSessionTriageResultView;

public interface AiSessionQueryRepository {

    Optional<AiSessionDetail> findSessionDetailById(Long sessionId);

    Optional<AiSessionTriageResultView> findLatestTriageResultBySessionId(Long sessionId);
}
