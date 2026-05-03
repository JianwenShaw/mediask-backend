package me.jianwen.mediask.infra.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;
import me.jianwen.mediask.infra.persistence.converter.InetStringTypeHandler;

@Getter
@Setter
@TableName(value = "audit.audit_event", autoResultMap = true)
public class AuditEventDO {

    @TableId(type = IdType.INPUT)
    private Long id;

    private String requestId;
    private String traceId;
    private String actorType;
    private Long operatorUserId;
    private String operatorUsername;
    private String operatorRoleCode;
    private Long actorDepartmentId;
    private String actionCode;
    private String resourceType;
    private String resourceId;
    private Long patientUserId;
    private Long encounterId;
    private Boolean successFlag;
    private String errorCode;
    private String errorMessage;
    @TableField(typeHandler = InetStringTypeHandler.class)
    private String clientIp;
    private String userAgent;
    private String reasonText;
    private OffsetDateTime occurredAt;
    private OffsetDateTime createdAt;
}
