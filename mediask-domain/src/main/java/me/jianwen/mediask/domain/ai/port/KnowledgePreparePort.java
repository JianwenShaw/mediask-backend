package me.jianwen.mediask.domain.ai.port;

import java.util.List;
import me.jianwen.mediask.domain.ai.model.KnowledgePrepareInvocation;
import me.jianwen.mediask.domain.ai.model.PreparedKnowledgeChunk;

public interface KnowledgePreparePort {

    List<PreparedKnowledgeChunk> prepare(KnowledgePrepareInvocation invocation);
}
