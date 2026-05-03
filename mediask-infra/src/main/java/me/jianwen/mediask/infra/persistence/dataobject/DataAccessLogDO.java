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
@TableName(value = "audit.data_access_log", autoResultMap = true)
public class DataAccessLogDO {

    @TableId(type = IdType.INPUT)
    private Long id;

    private String requestId;
    private String traceId;
    private String actorType;
    private Long operatorUserId;
    private String operatorUsername;
    private String operatorRoleCode;
    private Long actorDepartmentId;
    private Long patientUserId;
    private Long encounterId;
    private String accessAction;
    private String accessPurposeCode;
    private String resourceType;
    private String resourceId;
    private String accessResult;
    private String denyReasonCode;
    @TableField(typeHandler = InetStringTypeHandler.class)
    private String clientIp;
    private String userAgent;
    private OffsetDateTime occurredAt;
    private OffsetDateTime createdAt;
}
