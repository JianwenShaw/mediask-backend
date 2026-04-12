package me.jianwen.mediask.infra.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import me.jianwen.mediask.infra.persistence.dataobject.AiTurnDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface AiTurnMapper extends BaseMapper<AiTurnDO> {

    @Select("""
            SELECT COALESCE(MAX(turn_no), 0)
            FROM ai_turn
            WHERE session_id = #{sessionId}
              AND deleted_at IS NULL
            """)
    int selectMaxTurnNoBySessionId(@Param("sessionId") Long sessionId);
}
