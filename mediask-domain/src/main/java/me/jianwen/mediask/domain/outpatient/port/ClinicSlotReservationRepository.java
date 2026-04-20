package me.jianwen.mediask.domain.outpatient.port;

import java.util.Optional;
import me.jianwen.mediask.domain.outpatient.model.ClinicSlotReservation;

public interface ClinicSlotReservationRepository {

    boolean existsOpenSession(Long sessionId);

    Optional<ClinicSlotReservation> reserveAvailableSlot(Long sessionId, Long slotId);

    boolean releaseReservedSlot(Long sessionId, Long slotId, String expectedCurrentStatus);

    void refreshSessionRemainingCount(Long sessionId);
}
