package me.jianwen.mediask.infra.persistence.mapper;

import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AiSessionDetailRow {

    private Long sessionId;
    private Long patientId;
    private Long departmentId;
    private String sceneType;
    private String sessionStatus;
    private String chiefComplaintSummary;
    private String summary;
    private OffsetDateTime startedAt;
    private OffsetDateTime endedAt;
}
