package me.jianwen.mediask.infra.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("ai_guardrail_event")
public class AiGuardrailEventDO {

    @TableId(type = IdType.INPUT)
    private Long id;

    private Long runId;
    private String riskLevel;
    private String actionTaken;
    private String matchedRuleCodes;
    private String inputHash;
    private String outputHash;
    private String eventDetailJson;

    @TableField(fill = FieldFill.INSERT)
    private OffsetDateTime occurredAt;
}
