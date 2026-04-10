package me.jianwen.mediask.domain.ai.port;

public interface KnowledgeBaseRepository {

    boolean existsEnabled(Long knowledgeBaseId);
}
