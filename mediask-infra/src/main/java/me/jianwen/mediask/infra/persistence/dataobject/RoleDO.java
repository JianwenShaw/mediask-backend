package me.jianwen.mediask.infra.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import me.jianwen.mediask.infra.persistence.base.BaseDO;

@Getter
@Setter
@TableName("roles")
@EqualsAndHashCode(callSuper = true)
public class RoleDO extends BaseDO {

    private String roleCode;
    private String roleName;
    private String roleType;
    private String status;
    private Integer sortOrder;
}
