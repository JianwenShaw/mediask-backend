package me.jianwen.mediask.application.outpatient.usecase;

import me.jianwen.mediask.application.outpatient.command.CreateRegistrationCommand;
import me.jianwen.mediask.common.exception.BizException;
import me.jianwen.mediask.domain.outpatient.exception.OutpatientErrorCode;
import me.jianwen.mediask.domain.outpatient.model.ClinicSlotReservation;
import me.jianwen.mediask.domain.outpatient.model.RegistrationOrder;
import me.jianwen.mediask.domain.outpatient.port.ClinicSlotReservationRepository;
import me.jianwen.mediask.domain.outpatient.port.RegistrationOrderRepository;
import org.springframework.transaction.annotation.Transactional;

public class CreateRegistrationUseCase {

    private final ClinicSlotReservationRepository clinicSlotReservationRepository;
    private final RegistrationOrderRepository registrationOrderRepository;

    public CreateRegistrationUseCase(
            ClinicSlotReservationRepository clinicSlotReservationRepository,
            RegistrationOrderRepository registrationOrderRepository) {
        this.clinicSlotReservationRepository = clinicSlotReservationRepository;
        this.registrationOrderRepository = registrationOrderRepository;
    }

    @Transactional
    public CreateRegistrationResult handle(CreateRegistrationCommand command) {
        if (!clinicSlotReservationRepository.existsOpenSession(command.clinicSessionId())) {
            throw new BizException(OutpatientErrorCode.SESSION_NOT_FOUND);
        }

        ClinicSlotReservation reservation = clinicSlotReservationRepository
                .reserveAvailableSlot(command.clinicSessionId(), command.clinicSlotId())
                .orElseThrow(() -> new BizException(OutpatientErrorCode.SLOT_NOT_AVAILABLE));

        RegistrationOrder registrationOrder = RegistrationOrder.createPendingPayment(
                command.patientId(),
                reservation.doctorId(),
                reservation.departmentId(),
                reservation.sessionId(),
                reservation.slotId(),
                command.sourceAiSessionId(),
                reservation.fee());
        registrationOrderRepository.save(registrationOrder);
        clinicSlotReservationRepository.refreshSessionRemainingCount(command.clinicSessionId());
        return new CreateRegistrationResult(
                registrationOrder.registrationId(), registrationOrder.orderNo(), registrationOrder.status());
    }
}
