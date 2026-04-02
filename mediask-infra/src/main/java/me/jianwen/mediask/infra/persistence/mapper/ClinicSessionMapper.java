package me.jianwen.mediask.infra.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.time.LocalDate;
import java.util.List;
import me.jianwen.mediask.infra.persistence.dataobject.ClinicSessionDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ClinicSessionMapper extends BaseMapper<ClinicSessionDO> {

    List<ClinicSessionListRow> selectOpenClinicSessions(
            @Param("departmentId") Long departmentId,
            @Param("dateFrom") LocalDate dateFrom,
            @Param("dateTo") LocalDate dateTo);
}
