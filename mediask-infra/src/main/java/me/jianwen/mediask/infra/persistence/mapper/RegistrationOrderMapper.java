package me.jianwen.mediask.infra.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import me.jianwen.mediask.infra.persistence.dataobject.RegistrationOrderDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface RegistrationOrderMapper extends BaseMapper<RegistrationOrderDO> {}
