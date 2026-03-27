package me.jianwen.mediask.domain.ai.model;

import java.util.List;
import me.jianwen.mediask.common.util.ArgumentChecks;

public record AiExecutionMetadata(
        String providerRunId,
        List<String> matchedRuleCodes,
        Integer tokensInput,
        Integer tokensOutput,
        Integer latencyMs,
        boolean degraded) {

    public AiExecutionMetadata {
        providerRunId = ArgumentChecks.blankToNull(providerRunId);
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

}
