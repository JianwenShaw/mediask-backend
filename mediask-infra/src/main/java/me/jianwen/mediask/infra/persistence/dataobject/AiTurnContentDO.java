package me.jianwen.mediask.infra.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("ai_turn_content")
public class AiTurnContentDO {

    @TableId(type = IdType.INPUT)
    private Long id;

    private Long turnId;
    private String contentRole;
    private String contentEncrypted;
    private String contentMasked;
    private String contentHash;

    @TableField(fill = FieldFill.INSERT)
    private OffsetDateTime createdAt;
}
