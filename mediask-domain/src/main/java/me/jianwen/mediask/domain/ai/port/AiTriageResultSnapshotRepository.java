package me.jianwen.mediask.domain.ai.port;

import me.jianwen.mediask.domain.ai.model.AiTriageResultSnapshot;

public interface AiTriageResultSnapshotRepository {

    void save(AiTriageResultSnapshot snapshot);
}
