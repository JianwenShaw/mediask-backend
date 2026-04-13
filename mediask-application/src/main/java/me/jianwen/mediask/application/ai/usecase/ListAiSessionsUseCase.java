package me.jianwen.mediask.application.ai.usecase;

import java.util.List;
import me.jianwen.mediask.application.ai.query.ListAiSessionsQuery;
import me.jianwen.mediask.domain.ai.model.AiSessionListItem;
import me.jianwen.mediask.domain.ai.port.AiSessionQueryRepository;
import org.springframework.transaction.annotation.Transactional;

public class ListAiSessionsUseCase {

    private final AiSessionQueryRepository aiSessionQueryRepository;

    public ListAiSessionsUseCase(AiSessionQueryRepository aiSessionQueryRepository) {
        this.aiSessionQueryRepository = aiSessionQueryRepository;
    }

    @Transactional(readOnly = true)
    public List<AiSessionListItem> handle(ListAiSessionsQuery query) {
        return aiSessionQueryRepository.listSessionsByPatientUserId(query.patientUserId());
    }
}
