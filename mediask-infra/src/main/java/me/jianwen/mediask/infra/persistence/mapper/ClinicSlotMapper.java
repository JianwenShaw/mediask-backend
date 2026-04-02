package me.jianwen.mediask.infra.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import me.jianwen.mediask.infra.persistence.dataobject.ClinicSlotDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ClinicSlotMapper extends BaseMapper<ClinicSlotDO> {

    ClinicSlotReservationRow selectReservableSlot(
            @Param("sessionId") Long sessionId, @Param("slotId") Long slotId);

    Integer countAvailableSlots(@Param("sessionId") Long sessionId);
}
