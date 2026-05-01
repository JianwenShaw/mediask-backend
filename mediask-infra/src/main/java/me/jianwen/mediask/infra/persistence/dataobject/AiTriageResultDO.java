package me.jianwen.mediask.infra.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import me.jianwen.mediask.infra.persistence.base.BaseDO;
import me.jianwen.mediask.infra.persistence.converter.JsonbStringTypeHandler;

@Getter
@Setter
@TableName(value = "ai_triage_result", autoResultMap = true)
@EqualsAndHashCode(callSuper = true)
public class AiTriageResultDO extends BaseDO {

    private String requestId;
    private String sessionId;
    private String turnId;
    private String queryRunId;
    private String hospitalScope;
    private String triageStage;
    private String triageCompletionReason;
    private String nextAction;
    private String riskLevel;
    private String chiefComplaintSummary;
    private String careAdvice;
    private String blockedReason;
    private String catalogVersion;

    @TableField(typeHandler = JsonbStringTypeHandler.class)
    private String recommendedDepartmentsJson;

    @TableField(typeHandler = JsonbStringTypeHandler.class)
    private String citationsJson;
}
