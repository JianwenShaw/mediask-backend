package me.jianwen.mediask.infra.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import me.jianwen.mediask.infra.persistence.dataobject.AiModelRunDO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AiModelRunMapper extends BaseMapper<AiModelRunDO> {

    @Select("""
            SELECT turn.turn_no
            FROM ai_model_run run
            JOIN ai_turn turn ON turn.id = run.turn_id
            WHERE turn.session_id = #{sessionId}
              AND turn.deleted_at IS NULL
              AND run.deleted_at IS NULL
              AND run.triage_snapshot_json IS NOT NULL
            ORDER BY turn.turn_no DESC, run.id DESC
            LIMIT 1
            """)
    Integer selectLatestFinalizedTurnNoBySessionId(@Param("sessionId") Long sessionId);
}
