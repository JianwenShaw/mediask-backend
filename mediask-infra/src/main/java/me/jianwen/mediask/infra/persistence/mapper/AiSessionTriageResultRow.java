package me.jianwen.mediask.infra.persistence.mapper;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AiSessionTriageResultRow {

    private Long sessionId;
    private Long patientId;
    private Long modelRunId;
    private String sessionChiefComplaintSummary;
    private String riskLevel;
    private String guardrailAction;
    private String eventDetailJson;
}
