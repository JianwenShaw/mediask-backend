package me.jianwen.mediask.infra.persistence.mapper;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AiRunCitationRow {

    private Long chunkId;
    private Integer retrievalRank;
    private Double fusionScore;
    private String snippet;
}
