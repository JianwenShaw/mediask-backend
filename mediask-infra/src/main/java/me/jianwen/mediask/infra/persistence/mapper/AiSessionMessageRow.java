package me.jianwen.mediask.infra.persistence.mapper;

import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AiSessionMessageRow {

    private Long turnId;
    private String contentRole;
    private String contentEncrypted;
    private OffsetDateTime createdAt;
}
