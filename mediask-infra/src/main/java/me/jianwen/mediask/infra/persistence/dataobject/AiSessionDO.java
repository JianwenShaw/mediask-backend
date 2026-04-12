package me.jianwen.mediask.infra.persistence.dataobject;

import java.time.OffsetDateTime;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import me.jianwen.mediask.infra.persistence.base.BaseDO;
import com.baomidou.mybatisplus.annotation.TableName;

@Getter
@Setter
@TableName("ai_session")
@EqualsAndHashCode(callSuper = true)
public class AiSessionDO extends BaseDO {

    private String sessionUuid;
    private Long patientId;
    private Long departmentId;
    private String sceneType;
    private String entrypoint;
    private String sessionStatus;
    private String chiefComplaintSummary;
    private String summary;
    private OffsetDateTime startedAt;
    private OffsetDateTime endedAt;
}
