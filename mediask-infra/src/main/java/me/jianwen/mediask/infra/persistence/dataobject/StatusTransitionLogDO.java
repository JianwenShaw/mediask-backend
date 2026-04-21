package me.jianwen.mediask.infra.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("status_transition_log")
public class StatusTransitionLogDO {

    @TableId(type = IdType.INPUT)
    private Long id;

    private String entityType;
    private Long entityId;
    private String fromStatus;
    private String toStatus;
    private String action;
    private Long operatorUserId;
    private String requestId;
    private OffsetDateTime occurredAt;
}
