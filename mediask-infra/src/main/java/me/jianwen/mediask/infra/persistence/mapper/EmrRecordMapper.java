package me.jianwen.mediask.infra.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import me.jianwen.mediask.infra.persistence.dataobject.EmrRecordDO;
import me.jianwen.mediask.infra.persistence.dataobject.EmrDiagnosisDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

@Mapper
public interface EmrRecordMapper extends BaseMapper<EmrRecordDO> {

    int insertDiagnosis(EmrDiagnosisDO diagnosis);

    int insertDiagnoses(@Param("diagnoses") List<EmrDiagnosisDO> diagnoses);

    boolean existsByEncounterId(@Param("encounterId") Long encounterId);

    Optional<EmrRecordDO> selectByEncounterId(@Param("encounterId") Long encounterId);

    List<EmrRecordListRow> selectListByPatientUserId(
            @Param("patientUserId") Long patientUserId,
            @Param("excludeEncounterId") Long excludeEncounterId);

    Optional<Long> selectRecordIdByEncounterId(@Param("encounterId") Long encounterId);

    Optional<EmrRecordDO> selectAccessByRecordId(@Param("recordId") Long recordId);

    List<EmrDiagnosisDO> selectDiagnosesByRecordId(@Param("recordId") Long recordId);
}
