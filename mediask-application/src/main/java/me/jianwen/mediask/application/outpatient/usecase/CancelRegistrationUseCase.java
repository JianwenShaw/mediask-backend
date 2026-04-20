package me.jianwen.mediask.application.outpatient.usecase;

import java.time.OffsetDateTime;
import me.jianwen.mediask.application.outpatient.command.CancelRegistrationCommand;
import me.jianwen.mediask.common.exception.BizException;
import me.jianwen.mediask.domain.clinical.port.VisitEncounterRepository;
import me.jianwen.mediask.domain.outpatient.exception.OutpatientErrorCode;
import me.jianwen.mediask.domain.outpatient.model.RegistrationOrder;
import me.jianwen.mediask.domain.outpatient.port.ClinicSlotReservationRepository;
import me.jianwen.mediask.domain.outpatient.port.RegistrationOrderRepository;
import org.springframework.transaction.annotation.Transactional;

public class CancelRegistrationUseCase {

    private static final String SLOT_STATUS_LOCKED = "LOCKED";
    private static final String SLOT_STATUS_BOOKED = "BOOKED";

    private final RegistrationOrderRepository registrationOrderRepository;
    private final ClinicSlotReservationRepository clinicSlotReservationRepository;
    private final VisitEncounterRepository visitEncounterRepository;

    public CancelRegistrationUseCase(
            RegistrationOrderRepository registrationOrderRepository,
            ClinicSlotReservationRepository clinicSlotReservationRepository,
            VisitEncounterRepository visitEncounterRepository) {
        this.registrationOrderRepository = registrationOrderRepository;
        this.clinicSlotReservationRepository = clinicSlotReservationRepository;
        this.visitEncounterRepository = visitEncounterRepository;
    }

    @Transactional
    public CancelRegistrationResult handle(CancelRegistrationCommand command) {
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
                expectedSlotStatus(registrationOrder))) {
            throw new BizException(OutpatientErrorCode.REGISTRATION_CANCEL_NOT_ALLOWED);
        }

        registrationOrderRepository.update(cancelledOrder);
        clinicSlotReservationRepository.refreshSessionRemainingCount(registrationOrder.sessionId());
        return new CancelRegistrationResult(
                cancelledOrder.registrationId(), cancelledOrder.status(), cancelledOrder.cancelledAt());
    }

    private String expectedSlotStatus(RegistrationOrder registrationOrder) {
        return switch (registrationOrder.status()) {
            case PENDING_PAYMENT -> SLOT_STATUS_LOCKED;
            case CONFIRMED -> SLOT_STATUS_BOOKED;
            default -> throw new BizException(OutpatientErrorCode.INVALID_STATUS_TRANSITION);
        };
    }
}
