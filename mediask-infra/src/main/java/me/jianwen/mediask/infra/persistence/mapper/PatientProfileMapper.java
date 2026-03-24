package me.jianwen.mediask.infra.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import me.jianwen.mediask.infra.persistence.dataobject.PatientProfileDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PatientProfileMapper extends BaseMapper<PatientProfileDO> {
}
