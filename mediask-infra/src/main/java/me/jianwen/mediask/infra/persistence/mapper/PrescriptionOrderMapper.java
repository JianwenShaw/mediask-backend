package me.jianwen.mediask.infra.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.List;
import java.util.Optional;
import me.jianwen.mediask.infra.persistence.dataobject.PrescriptionItemDO;
import me.jianwen.mediask.infra.persistence.dataobject.PrescriptionOrderDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface PrescriptionOrderMapper extends BaseMapper<PrescriptionOrderDO> {

    int insertItems(@Param("items") List<PrescriptionItemDO> items);

    boolean existsByEncounterId(@Param("encounterId") Long encounterId);

    Optional<PrescriptionOrderDO> selectByEncounterId(@Param("encounterId") Long encounterId);

    List<PrescriptionItemDO> selectItemsByPrescriptionId(@Param("prescriptionId") Long prescriptionId);

    Optional<PrescriptionOrderDO> selectById(@Param("id") Long id);

    int deleteItemsByPrescriptionId(@Param("prescriptionId") Long prescriptionId);
}
