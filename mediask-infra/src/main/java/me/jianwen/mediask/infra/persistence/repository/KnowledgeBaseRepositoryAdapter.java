package me.jianwen.mediask.infra.persistence.repository;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import me.jianwen.mediask.domain.ai.port.KnowledgeBaseRepository;
import me.jianwen.mediask.infra.persistence.dataobject.KnowledgeBaseDO;
import me.jianwen.mediask.infra.persistence.mapper.KnowledgeBaseMapper;
import org.springframework.stereotype.Component;

@Component
public class KnowledgeBaseRepositoryAdapter implements KnowledgeBaseRepository {

    private final KnowledgeBaseMapper knowledgeBaseMapper;

    public KnowledgeBaseRepositoryAdapter(KnowledgeBaseMapper knowledgeBaseMapper) {
        this.knowledgeBaseMapper = knowledgeBaseMapper;
    }

    @Override
    public boolean existsEnabled(Long knowledgeBaseId) {
        return knowledgeBaseMapper.selectCount(Wrappers.lambdaQuery(KnowledgeBaseDO.class)
                        .eq(KnowledgeBaseDO::getId, knowledgeBaseId)
                        .eq(KnowledgeBaseDO::getStatus, "ENABLED")
                        .isNull(KnowledgeBaseDO::getDeletedAt))
                > 0;
    }
}
