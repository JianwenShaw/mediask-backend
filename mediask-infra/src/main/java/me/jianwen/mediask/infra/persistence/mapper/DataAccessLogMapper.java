package me.jianwen.mediask.infra.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.time.OffsetDateTime;
import java.util.List;
import me.jianwen.mediask.infra.persistence.dataobject.DataAccessLogDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface DataAccessLogMapper extends BaseMapper<DataAccessLogDO> {

    List<DataAccessLogDO> selectDataAccessLogs(
            @Param("from") OffsetDateTime from,
            @Param("to") OffsetDateTime to,
            @Param("resourceType") String resourceType,
            @Param("resourceId") String resourceId,
            @Param("operatorUserId") Long operatorUserId,
            @Param("patientUserId") Long patientUserId,
            @Param("encounterId") Long encounterId,
            @Param("accessAction") String accessAction,
            @Param("accessResult") String accessResult,
            @Param("requestId") String requestId,
            @Param("limit") long limit,
            @Param("offset") long offset);

    long countDataAccessLogs(
            @Param("from") OffsetDateTime from,
            @Param("to") OffsetDateTime to,
            @Param("resourceType") String resourceType,
            @Param("resourceId") String resourceId,
            @Param("operatorUserId") Long operatorUserId,
            @Param("patientUserId") Long patientUserId,
            @Param("encounterId") Long encounterId,
            @Param("accessAction") String accessAction,
            @Param("accessResult") String accessResult,
            @Param("requestId") String requestId);
}
