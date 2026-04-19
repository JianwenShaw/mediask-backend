package me.jianwen.mediask.application.clinical.usecase;

import me.jianwen.mediask.application.clinical.query.GetEmrDetailQuery;
import me.jianwen.mediask.common.exception.BizException;
import me.jianwen.mediask.domain.clinical.exception.ClinicalErrorCode;
import me.jianwen.mediask.domain.clinical.model.EmrRecord;
import me.jianwen.mediask.domain.clinical.port.EmrRecordQueryRepository;
import org.springframework.transaction.annotation.Transactional;

public class GetEmrDetailUseCase {

    private final EmrRecordQueryRepository emrRecordQueryRepository;

    public GetEmrDetailUseCase(EmrRecordQueryRepository emrRecordQueryRepository) {
        this.emrRecordQueryRepository = emrRecordQueryRepository;
    }

    @Transactional(readOnly = true)
    public EmrRecord handle(GetEmrDetailQuery query) {
        return emrRecordQueryRepository
                .findByEncounterId(query.encounterId())
                .orElseThrow(() -> new BizException(ClinicalErrorCode.EMR_RECORD_NOT_FOUND));
    }
}
