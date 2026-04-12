package me.jianwen.mediask.infra.persistence.mapper;

import me.jianwen.mediask.infra.persistence.dataobject.AiGuardrailEventDO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AiGuardrailEventMapper {

    @Insert("""
            INSERT INTO ai_guardrail_event (
                id,
                run_id,
                risk_level,
                action_taken,
                matched_rule_codes,
                input_hash,
                output_hash,
                event_detail_json,
                occurred_at
            ) VALUES (
                #{event.id},
                #{event.runId},
                #{event.riskLevel},
                #{event.actionTaken},
                CAST(#{event.matchedRuleCodes} AS JSONB),
                #{event.inputHash},
                #{event.outputHash},
                CAST(#{event.eventDetailJson} AS JSONB),
                CURRENT_TIMESTAMP
            )
            """)
    int insertAiGuardrailEvent(@Param("event") AiGuardrailEventDO event);
}
