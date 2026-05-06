package me.jianwen.mediask.infra.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.OffsetDateTime;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import me.jianwen.mediask.infra.persistence.base.BaseDO;

@Getter
@Setter
@TableName("users")
@EqualsAndHashCode(callSuper = true)
public class UserDO extends BaseDO {

    private String username;
    private String phone;
    private String passwordHash;
    private String displayName;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String mobileMasked;
    private String userType;
    private String accountStatus;
    private OffsetDateTime lastLoginAt;
}
