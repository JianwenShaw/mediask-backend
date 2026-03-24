package me.jianwen.mediask.infra.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import me.jianwen.mediask.infra.persistence.base.BaseDO;

@Getter
@Setter
@TableName("permissions")
@EqualsAndHashCode(callSuper = true)
public class PermissionDO extends BaseDO {

    private String permissionCode;
    private String permissionName;
    private String permissionType;
    private Long parentId;
    private String resourcePath;
    private String httpMethod;
    private String status;
    private Integer sortOrder;
}
