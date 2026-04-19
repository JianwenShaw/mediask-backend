package me.jianwen.mediask.infra.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import me.jianwen.mediask.infra.persistence.dataobject.EmrRecordContentDO;
import me.jianwen.mediask.infra.persistence.dataobject.EmrRecordDO;
import me.jianwen.mediask.infra.persistence.dataobject.EmrDiagnosisDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

@Mapper
public interface EmrRecordMapper extends BaseMapper<EmrRecordDO> {

    int insertContent(EmrRecordContentDO content);

    int insertDiagnosis(EmrDiagnosisDO diagnosis);

    int insertDiagnoses(@Param("diagnoses") List<EmrDiagnosisDO> diagnoses);

    boolean existsByEncounterId(@Param("encounterId") Long encounterId);

    Optional<EmrRecordDO> selectByEncounterId(@Param("encounterId") Long encounterId);

    Optional<Long> selectRecordIdByEncounterId(@Param("encounterId") Long encounterId);

    Optional<EmrRecordDO> selectAccessByRecordId(@Param("recordId") Long recordId);

    Optional<EmrRecordContentDO> selectContentByRecordId(@Param("recordId") Long recordId);

    List<EmrDiagnosisDO> selectDiagnosesByRecordId(@Param("recordId") Long recordId);
}
