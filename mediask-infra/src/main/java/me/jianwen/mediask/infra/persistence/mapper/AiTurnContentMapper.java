package me.jianwen.mediask.infra.persistence.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import me.jianwen.mediask.infra.persistence.dataobject.AiTurnContentDO;

@Mapper
public interface AiTurnContentMapper {

    @Insert("""
            INSERT INTO ai_turn_content (
                id,
                turn_id,
                content_role,
                content_encrypted,
                content_masked,
                content_hash,
                created_at
            ) VALUES (
                #{content.id},
                #{content.turnId},
                #{content.contentRole},
                #{content.contentEncrypted},
                #{content.contentMasked},
                #{content.contentHash},
                CURRENT_TIMESTAMP
            )
            """)
    int insertAiTurnContent(@Param("content") AiTurnContentDO content);
}
