package me.jianwen.mediask.infra.persistence.dataobject;

import java.time.OffsetDateTime;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import me.jianwen.mediask.infra.persistence.base.BaseDO;
import com.baomidou.mybatisplus.annotation.TableName;
import me.jianwen.mediask.infra.persistence.converter.JsonbStringTypeHandler;

@Getter
@Setter
@TableName(value = "ai_model_run", autoResultMap = true)
@EqualsAndHashCode(callSuper = true)
public class AiModelRunDO extends BaseDO {

    private Long turnId;
    private String providerName;
    private String providerRunId;
    private String modelName;
    private String requestId;
    private Boolean ragEnabled;
    private String retrievalProvider;
    private String runStatus;
    private Boolean isDegraded;
    private Integer tokensInput;
    private Integer tokensOutput;
    private Integer latencyMs;
    private String requestPayloadHash;
    private String responsePayloadHash;
    private Integer errorCode;
    private String errorMessage;
    @TableField(typeHandler = JsonbStringTypeHandler.class)
    private String triageSnapshotJson;
    private OffsetDateTime startedAt;
    private OffsetDateTime completedAt;
}
