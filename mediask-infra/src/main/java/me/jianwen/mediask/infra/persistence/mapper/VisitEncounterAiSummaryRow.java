package me.jianwen.mediask.infra.persistence.mapper;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VisitEncounterAiSummaryRow {

    private Long encounterId;
    private Long sessionId;
    private String structuredSummary;
    private Long modelRunId;
    private String riskLevel;
    private String triageSnapshotJson;
}
