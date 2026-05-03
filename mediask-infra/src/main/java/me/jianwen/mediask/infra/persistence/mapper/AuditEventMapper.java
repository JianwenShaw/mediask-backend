package me.jianwen.mediask.infra.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.time.OffsetDateTime;
import java.util.List;
import me.jianwen.mediask.infra.persistence.dataobject.AuditEventDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AuditEventMapper extends BaseMapper<AuditEventDO> {

    List<AuditEventDO> selectAuditEvents(
            @Param("from") OffsetDateTime from,
            @Param("to") OffsetDateTime to,
            @Param("actionCode") String actionCode,
            @Param("operatorUserId") Long operatorUserId,
            @Param("patientUserId") Long patientUserId,
            @Param("encounterId") Long encounterId,
            @Param("resourceType") String resourceType,
            @Param("resourceId") String resourceId,
            @Param("successFlag") Boolean successFlag,
            @Param("requestId") String requestId,
            @Param("limit") long limit,
            @Param("offset") long offset);

    long countAuditEvents(
            @Param("from") OffsetDateTime from,
            @Param("to") OffsetDateTime to,
            @Param("actionCode") String actionCode,
            @Param("operatorUserId") Long operatorUserId,
            @Param("patientUserId") Long patientUserId,
            @Param("encounterId") Long encounterId,
            @Param("resourceType") String resourceType,
            @Param("resourceId") String resourceId,
            @Param("successFlag") Boolean successFlag,
            @Param("requestId") String requestId);
}
