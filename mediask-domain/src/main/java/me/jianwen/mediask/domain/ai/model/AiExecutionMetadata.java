package me.jianwen.mediask.domain.ai.model;

import java.util.List;

public record AiExecutionMetadata(
        String providerRunId,
        List<String> matchedRuleCodes,
        Integer tokensInput,
        Integer tokensOutput,
        Integer latencyMs,
        boolean degraded) {

    public AiExecutionMetadata {
        providerRunId = normalizeBlank(providerRunId);
        matchedRuleCodes = matchedRuleCodes == null
                ? List.of()
                : matchedRuleCodes.stream().filter(code -> code != null && !code.isBlank()).map(String::trim).toList();
        tokensInput = normalizeNonNegative(tokensInput, "tokensInput");
        tokensOutput = normalizeNonNegative(tokensOutput, "tokensOutput");
        latencyMs = normalizeNonNegative(latencyMs, "latencyMs");
    }

    public static AiExecutionMetadata empty() {
        return new AiExecutionMetadata(null, List.of(), null, null, null, false);
    }

    private static Integer normalizeNonNegative(Integer value, String fieldName) {
        if (value == null) {
            return null;
        }
        if (value < 0) {
            throw new IllegalArgumentException(fieldName + " must not be negative");
        }
        return value;
    }

    private static String normalizeBlank(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
