package me.jianwen.mediask.infra.persistence.mapper;

import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AiSessionTurnRow {

    private Long turnId;
    private Integer turnNo;
    private String turnStatus;
    private OffsetDateTime startedAt;
    private OffsetDateTime completedAt;
    private Integer errorCode;
    private String errorMessage;
}
