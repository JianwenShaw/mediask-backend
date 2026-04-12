package me.jianwen.mediask.infra.persistence.dataobject;

import java.time.OffsetDateTime;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import me.jianwen.mediask.infra.persistence.base.BaseDO;
import com.baomidou.mybatisplus.annotation.TableName;

@Getter
@Setter
@TableName("ai_turn")
@EqualsAndHashCode(callSuper = true)
public class AiTurnDO extends BaseDO {

    private Long sessionId;
    private Integer turnNo;
    private String turnStatus;
    private String inputHash;
    private String outputHash;
    private OffsetDateTime startedAt;
    private OffsetDateTime completedAt;
    private Integer errorCode;
    private String errorMessage;
}
