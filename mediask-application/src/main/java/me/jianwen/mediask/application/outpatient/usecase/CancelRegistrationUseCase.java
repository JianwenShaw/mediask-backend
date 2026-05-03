package me.jianwen.mediask.application.outpatient.usecase;

import java.time.OffsetDateTime;
import me.jianwen.mediask.application.audit.AuditActionCodes;
import me.jianwen.mediask.application.audit.AuditResourceTypes;
import me.jianwen.mediask.application.audit.model.AuditContext;
import me.jianwen.mediask.application.outpatient.command.CancelRegistrationCommand;
import me.jianwen.mediask.common.exception.BizException;
import me.jianwen.mediask.domain.clinical.port.VisitEncounterRepository;
import me.jianwen.mediask.domain.outpatient.exception.OutpatientErrorCode;
import me.jianwen.mediask.domain.outpatient.model.RegistrationOrder;
import me.jianwen.mediask.domain.outpatient.port.ClinicSlotReservationRepository;
import me.jianwen.mediask.domain.outpatient.port.RegistrationOrderRepository;
import org.springframework.transaction.annotation.Transactional;

public class CancelRegistrationUseCase {

    private static final String SLOT_STATUS_BOOKED = "BOOKED";

    private final RegistrationOrderRepository registrationOrderRepository;
    private final ClinicSlotReservationRepository clinicSlotReservationRepository;
    private final VisitEncounterRepository visitEncounterRepository;
    private final me.jianwen.mediask.application.audit.usecase.AuditTrailService auditTrailService;

    public CancelRegistrationUseCase(
            RegistrationOrderRepository registrationOrderRepository,
            ClinicSlotReservationRepository clinicSlotReservationRepository,
            VisitEncounterRepository visitEncounterRepository,
            me.jianwen.mediask.application.audit.usecase.AuditTrailService auditTrailService) {
        this.registrationOrderRepository = registrationOrderRepository;
        this.clinicSlotReservationRepository = clinicSlotReservationRepository;
        this.visitEncounterRepository = visitEncounterRepository;
        this.auditTrailService = auditTrailService;
    }

    @Transactional
    public CancelRegistrationResult handle(CancelRegistrationCommand command, AuditContext auditContext) {
        RegistrationOrder registrationOrder = registrationOrderRepository
                .findByRegistrationIdAndPatientId(command.registrationId(), command.patientUserId())
                .orElseThrow(() -> new BizException(OutpatientErrorCode.REGISTRATION_NOT_FOUND));

        OffsetDateTime cancelledAt = OffsetDateTime.now();
        RegistrationOrder cancelledOrder = registrationOrder.cancel(cancelledAt);
        if (!visitEncounterRepository.cancelScheduledByRegistrationId(registrationOrder.registrationId())) {
            throw new BizException(OutpatientErrorCode.REGISTRATION_CANCEL_NOT_ALLOWED);
        }
        if (!clinicSlotReservationRepository.releaseReservedSlot(
                registrationOrder.sessionId(),
                registrationOrder.slotId(),
                SLOT_STATUS_BOOKED)) {
            throw new BizException(OutpatientErrorCode.REGISTRATION_CANCEL_NOT_ALLOWED);
        }

        registrationOrderRepository.update(cancelledOrder);
        clinicSlotReservationRepository.refreshSessionRemainingCount(registrationOrder.sessionId());
        CancelRegistrationResult result = new CancelRegistrationResult(
                cancelledOrder.registrationId(), cancelledOrder.status(), cancelledOrder.cancelledAt());
        auditTrailService.recordAuditSuccess(
                auditContext,
                AuditActionCodes.REGISTRATION_CANCEL,
                AuditResourceTypes.REGISTRATION_ORDER,
                String.valueOf(cancelledOrder.registrationId()),
                cancelledOrder.patientId(),
                null,
                null);
        return result;
    }
}
