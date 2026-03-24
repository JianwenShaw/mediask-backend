package me.jianwen.mediask.infra.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import me.jianwen.mediask.infra.persistence.base.BaseDO;

@Getter
@Setter
@TableName("departments")
@EqualsAndHashCode(callSuper = true)
public class DepartmentDO extends BaseDO {

    private Long hospitalId;
    private String deptCode;
    private String name;
    private String deptType;
    private String status;
    private Integer sortOrder;
}
