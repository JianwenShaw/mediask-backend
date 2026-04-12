package me.jianwen.mediask.infra.persistence.repository;

import me.jianwen.mediask.domain.ai.model.AiTurnContent;
import me.jianwen.mediask.domain.ai.port.AiTurnContentRepository;
import me.jianwen.mediask.infra.persistence.dataobject.AiTurnContentDO;
import me.jianwen.mediask.infra.persistence.mapper.AiTurnContentMapper;
import org.springframework.stereotype.Component;

@Component
public class AiTurnContentRepositoryAdapter implements AiTurnContentRepository {

    private final AiTurnContentMapper aiTurnContentMapper;

    public AiTurnContentRepositoryAdapter(AiTurnContentMapper aiTurnContentMapper) {
        this.aiTurnContentMapper = aiTurnContentMapper;
    }

    @Override
    public void save(AiTurnContent aiTurnContent) {
        AiTurnContentDO dataObject = new AiTurnContentDO();
        dataObject.setId(aiTurnContent.id());
        dataObject.setTurnId(aiTurnContent.turnId());
        dataObject.setContentRole(aiTurnContent.role().name());
        dataObject.setContentEncrypted(aiTurnContent.encryptedContent());
        dataObject.setContentMasked(aiTurnContent.maskedContent());
        dataObject.setContentHash(aiTurnContent.contentHash());
        aiTurnContentMapper.insertAiTurnContent(dataObject);
    }
}
