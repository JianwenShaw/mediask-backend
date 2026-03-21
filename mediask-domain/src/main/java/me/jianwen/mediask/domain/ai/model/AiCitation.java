package me.jianwen.mediask.domain.ai.model;

public record AiCitation(Long chunkId, Integer retrievalRank, Double fusionScore, String snippet) {

    public AiCitation {
        chunkId = requirePositive(chunkId, "chunkId");
        retrievalRank = normalizePositive(retrievalRank, "retrievalRank");
        snippet = requireNonBlank(snippet, "snippet");
    }

    private static Long requirePositive(Long value, String fieldName) {
        if (value == null || value <= 0L) {
            throw new IllegalArgumentException(fieldName + " must be greater than 0");
        }
        return value;
    }

    private static Integer normalizePositive(Integer value, String fieldName) {
        if (value == null) {
            return null;
        }
        if (value <= 0) {
            throw new IllegalArgumentException(fieldName + " must be greater than 0");
        }
        return value;
    }

    private static String requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }
}
