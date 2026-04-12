package me.jianwen.mediask.domain.ai.port;

import me.jianwen.mediask.domain.ai.model.AiGuardrailEvent;

public interface AiGuardrailEventRepository {

    void save(AiGuardrailEvent aiGuardrailEvent);
}
