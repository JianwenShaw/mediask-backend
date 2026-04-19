package me.jianwen.mediask.infra.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import me.jianwen.mediask.infra.persistence.dataobject.EmrRecordContentDO;
import me.jianwen.mediask.infra.persistence.dataobject.EmrRecordDO;
import me.jianwen.mediask.infra.persistence.dataobject.EmrDiagnosisDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface EmrRecordMapper extends BaseMapper<EmrRecordDO> {

    int insertContent(EmrRecordContentDO content);

    int insertDiagnosis(EmrDiagnosisDO diagnosis);

    int insertDiagnoses(@Param("diagnoses") List<EmrDiagnosisDO> diagnoses);

    boolean existsByEncounterId(@Param("encounterId") Long encounterId);
}