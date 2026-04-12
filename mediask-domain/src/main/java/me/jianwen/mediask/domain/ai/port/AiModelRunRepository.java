package me.jianwen.mediask.domain.ai.port;

import me.jianwen.mediask.domain.ai.model.AiModelRun;

public interface AiModelRunRepository {

    void save(AiModelRun aiModelRun);

    void update(AiModelRun aiModelRun);
}
