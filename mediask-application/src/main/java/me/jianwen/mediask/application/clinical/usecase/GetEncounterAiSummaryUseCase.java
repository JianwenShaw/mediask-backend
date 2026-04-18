package me.jianwen.mediask.application.clinical.usecase;

import me.jianwen.mediask.application.clinical.query.GetEncounterAiSummaryQuery;
import me.jianwen.mediask.common.exception.BizException;
import me.jianwen.mediask.domain.clinical.exception.ClinicalErrorCode;
import me.jianwen.mediask.domain.clinical.model.EncounterAiSummary;
import me.jianwen.mediask.domain.clinical.model.EncounterDetail;
import me.jianwen.mediask.domain.clinical.port.EncounterQueryRepository;
import org.springframework.transaction.annotation.Transactional;

public class GetEncounterAiSummaryUseCase {

    private final EncounterQueryRepository encounterQueryRepository;

    public GetEncounterAiSummaryUseCase(EncounterQueryRepository encounterQueryRepository) {
        this.encounterQueryRepository = encounterQueryRepository;
    }

    @Transactional(readOnly = true)
    public EncounterAiSummary handle(GetEncounterAiSummaryQuery query) {
        EncounterDetail detail = encounterQueryRepository
                .findDetailByEncounterId(query.encounterId())
                .orElseThrow(() -> new BizException(ClinicalErrorCode.ENCOUNTER_NOT_FOUND));
        if (!detail.doctorId().equals(query.doctorId())) {
            throw new BizException(ClinicalErrorCode.ENCOUNTER_ACCESS_DENIED);
        }
        return encounterQueryRepository
                .findAiSummaryByEncounterId(query.encounterId())
                .orElseThrow(() -> new BizException(ClinicalErrorCode.ENCOUNTER_AI_SUMMARY_NOT_FOUND));
    }
}
