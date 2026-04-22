package me.jianwen.mediask.application.clinical.usecase;

import me.jianwen.mediask.application.clinical.query.GetPrescriptionDetailQuery;
import me.jianwen.mediask.common.exception.BizException;
import me.jianwen.mediask.domain.clinical.exception.ClinicalErrorCode;
import me.jianwen.mediask.domain.clinical.model.PrescriptionOrder;
import me.jianwen.mediask.domain.clinical.port.EncounterQueryRepository;
import me.jianwen.mediask.domain.clinical.port.PrescriptionOrderQueryRepository;

public class GetPrescriptionDetailUseCase {

    private final PrescriptionOrderQueryRepository prescriptionOrderQueryRepository;
    private final EncounterQueryRepository encounterQueryRepository;

    public GetPrescriptionDetailUseCase(
            PrescriptionOrderQueryRepository prescriptionOrderQueryRepository,
            EncounterQueryRepository encounterQueryRepository) {
        this.prescriptionOrderQueryRepository = prescriptionOrderQueryRepository;
        this.encounterQueryRepository = encounterQueryRepository;
    }

    public PrescriptionOrder handle(GetPrescriptionDetailQuery query) {
        var encounter = encounterQueryRepository.findDetailByEncounterId(query.encounterId())
                .orElseThrow(() -> new BizException(ClinicalErrorCode.PRESCRIPTION_ENCOUNTER_NOT_FOUND));
        if (!encounter.doctorId().equals(query.doctorId())) {
            throw new BizException(ClinicalErrorCode.PRESCRIPTION_ENCOUNTER_NOT_FOUND);
        }
        return prescriptionOrderQueryRepository.findByEncounterId(query.encounterId())
                .orElseThrow(() -> new BizException(ClinicalErrorCode.PRESCRIPTION_NOT_FOUND));
    }
}
