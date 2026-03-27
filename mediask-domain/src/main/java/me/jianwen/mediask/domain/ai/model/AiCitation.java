package me.jianwen.mediask.domain.ai.model;

import me.jianwen.mediask.common.util.ArgumentChecks;

public record AiCitation(Long chunkId, Integer retrievalRank, Double fusionScore, String snippet) {

    public AiCitation {
        chunkId = ArgumentChecks.requirePositive(chunkId, "chunkId");
        retrievalRank = ArgumentChecks.normalizePositive(retrievalRank, "retrievalRank");
        snippet = ArgumentChecks.requireNonBlank(snippet, "snippet");
    }
}
