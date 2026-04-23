package me.jianwen.mediask.infra.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.List;
import me.jianwen.mediask.infra.persistence.dataobject.VisitEncounterDO;
import java.time.OffsetDateTime;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface VisitEncounterMapper extends BaseMapper<VisitEncounterDO> {

    List<VisitEncounterListRow> selectDoctorEncounters(
            @Param("doctorId") Long doctorId, @Param("status") String status);

    VisitEncounterDetailRow selectEncounterDetail(@Param("encounterId") Long encounterId);

    int startEncounterWhenScheduledAndRegistrationConfirmed(
            @Param("encounterId") Long encounterId, @Param("startedAt") OffsetDateTime startedAt);
}
