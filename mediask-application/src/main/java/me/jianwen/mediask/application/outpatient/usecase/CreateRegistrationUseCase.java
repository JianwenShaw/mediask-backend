package me.jianwen.mediask.application.outpatient.usecase;

import me.jianwen.mediask.application.outpatient.command.CreateRegistrationCommand;
import me.jianwen.mediask.common.exception.BizException;
import me.jianwen.mediask.domain.clinical.model.VisitEncounter;
import me.jianwen.mediask.domain.clinical.port.VisitEncounterRepository;
import me.jianwen.mediask.domain.outpatient.exception.OutpatientErrorCode;
import me.jianwen.mediask.domain.outpatient.model.ClinicSlotReservation;
import me.jianwen.mediask.domain.outpatient.model.RegistrationOrder;
import me.jianwen.mediask.domain.outpatient.port.ClinicSlotReservationRepository;
import me.jianwen.mediask.domain.outpatient.port.RegistrationOrderRepository;
import org.springframework.transaction.annotation.Transactional;

public class CreateRegistrationUseCase {

    private final ClinicSlotReservationRepository clinicSlotReservationRepository;
    private final RegistrationOrderRepository registrationOrderRepository;
    private final VisitEncounterRepository visitEncounterRepository;

    public CreateRegistrationUseCase(
            ClinicSlotReservationRepository clinicSlotReservationRepository,
            RegistrationOrderRepository registrationOrderRepository,
            VisitEncounterRepository visitEncounterRepository) {
        this.clinicSlotReservationRepository = clinicSlotReservationRepository;
        this.registrationOrderRepository = registrationOrderRepository;
        this.visitEncounterRepository = visitEncounterRepository;
    }

    @Transactional
    public CreateRegistrationResult handle(CreateRegistrationCommand command) {
        if (!clinicSlotReservationRepository.existsOpenSession(command.clinicSessionId())) {
            throw new BizException(OutpatientErrorCode.SESSION_NOT_FOUND);
        }

        ClinicSlotReservation reservation = clinicSlotReservationRepository
                .reserveAvailableSlot(command.clinicSessionId(), command.clinicSlotId())
                .orElseThrow(() -> new BizException(OutpatientErrorCode.SLOT_NOT_AVAILABLE));

        RegistrationOrder registrationOrder = RegistrationOrder.createConfirmed(
                command.patientUserId(),
                reservation.doctorId(),
                reservation.departmentId(),
                reservation.sessionId(),
                reservation.slotId(),
                reservation.fee());
        registrationOrderRepository.save(registrationOrder);
        visitEncounterRepository.save(VisitEncounter.createScheduled(
                registrationOrder.registrationId(),
                registrationOrder.patientId(),
                registrationOrder.doctorId(),
                registrationOrder.departmentId()));
        clinicSlotReservationRepository.refreshSessionRemainingCount(command.clinicSessionId());
        return new CreateRegistrationResult(
                registrationOrder.registrationId(), registrationOrder.orderNo(), registrationOrder.status());
    }
}
