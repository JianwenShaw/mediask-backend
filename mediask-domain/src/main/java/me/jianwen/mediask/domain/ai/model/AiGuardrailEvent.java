package me.jianwen.mediask.domain.ai.model;

import java.util.List;
import me.jianwen.mediask.common.id.SnowflakeIdGenerator;
import me.jianwen.mediask.common.util.ArgumentChecks;

public record AiGuardrailEvent(
        Long id,
        Long runId,
        RiskLevel riskLevel,
        GuardrailAction actionTaken,
        List<String> matchedRuleCodes,
        String inputHash,
        String outputHash) {

    public AiGuardrailEvent {
        id = ArgumentChecks.requirePositive(id, "id");
        runId = ArgumentChecks.requirePositive(runId, "runId");
        matchedRuleCodes = matchedRuleCodes == null ? List.of() : List.copyOf(matchedRuleCodes);
        inputHash = ArgumentChecks.blankToNull(inputHash);
        outputHash = ArgumentChecks.blankToNull(outputHash);
    }

    public static AiGuardrailEvent create(
            Long runId,
            RiskLevel riskLevel,
            GuardrailAction actionTaken,
            List<String> matchedRuleCodes,
            String inputHash,
            String outputHash) {
        return new AiGuardrailEvent(
                SnowflakeIdGenerator.nextId(),
                runId,
                riskLevel,
                actionTaken,
                matchedRuleCodes,
                inputHash,
                outputHash);
    }
}
