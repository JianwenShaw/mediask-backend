package me.jianwen.mediask.infra.persistence.mapper;

import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AiSessionTriageResultRow {

    private Long sessionId;
    private Long patientId;
    private Long modelRunId;
    private Long finalizedTurnId;
    private Integer finalizedTurnNo;
    private OffsetDateTime finalizedAt;
    private Integer latestTurnNo;
    private String latestTurnStatus;
    private String latestRunStatus;
    private String latestEventDetailJson;
    private String riskLevel;
    private String guardrailAction;
    private String triageSnapshotJson;
}
