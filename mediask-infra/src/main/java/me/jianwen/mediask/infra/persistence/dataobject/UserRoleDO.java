package me.jianwen.mediask.infra.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.TableName;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("user_roles")
public class UserRoleDO {

    private Long id;
    private Long userId;
    private Long roleId;
    private Long grantedBy;
    private OffsetDateTime grantedAt;
    private OffsetDateTime expiresAt;
    private Boolean activeFlag;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
