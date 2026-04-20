package me.jianwen.mediask.application.outpatient.usecase;

import me.jianwen.mediask.application.outpatient.query.GetRegistrationDetailQuery;
import me.jianwen.mediask.common.exception.BizException;
import me.jianwen.mediask.domain.outpatient.exception.OutpatientErrorCode;
import me.jianwen.mediask.domain.outpatient.model.RegistrationDetail;
import me.jianwen.mediask.domain.outpatient.port.RegistrationOrderQueryRepository;
import org.springframework.transaction.annotation.Transactional;

public class GetRegistrationDetailUseCase {

    private final RegistrationOrderQueryRepository registrationOrderQueryRepository;

    public GetRegistrationDetailUseCase(RegistrationOrderQueryRepository registrationOrderQueryRepository) {
        this.registrationOrderQueryRepository = registrationOrderQueryRepository;
    }

    @Transactional(readOnly = true)
    public RegistrationDetail handle(GetRegistrationDetailQuery query) {
        return registrationOrderQueryRepository
                .findDetailByPatientUserIdAndRegistrationId(query.patientUserId(), query.registrationId())
                .orElseThrow(() -> new BizException(OutpatientErrorCode.REGISTRATION_NOT_FOUND));
    }
}
