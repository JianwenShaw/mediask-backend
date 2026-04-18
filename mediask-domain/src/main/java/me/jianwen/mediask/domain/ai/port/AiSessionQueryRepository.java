package me.jianwen.mediask.domain.ai.port;

import java.util.List;
import java.util.Optional;
import me.jianwen.mediask.domain.ai.model.AiSessionDetail;
import me.jianwen.mediask.domain.ai.model.AiSessionListItem;
import me.jianwen.mediask.domain.ai.model.AiTriageStage;
import me.jianwen.mediask.domain.ai.model.AiSessionTriageResultView;

public interface AiSessionQueryRepository {

    List<AiSessionListItem> listSessionsByPatientUserId(Long patientUserId);

    Optional<AiSessionDetail> findSessionDetailById(Long sessionId);

    Optional<AiSessionTriageResultView> findLatestTriageResultBySessionId(Long sessionId);

    Optional<AiTriageStage> findLatestTriageStageBySessionId(Long sessionId);

    boolean hasAccessibleTriageSession(Long patientUserId, Long sessionId);
}
