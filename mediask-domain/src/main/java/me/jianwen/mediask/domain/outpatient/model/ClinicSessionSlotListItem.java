package me.jianwen.mediask.domain.outpatient.model;

import java.time.OffsetDateTime;
import me.jianwen.mediask.common.util.ArgumentChecks;

public record ClinicSessionSlotListItem(
        Long clinicSlotId, Integer slotSeq, OffsetDateTime slotStartTime, OffsetDateTime slotEndTime) {

    public ClinicSessionSlotListItem {
        clinicSlotId = ArgumentChecks.requirePositive(clinicSlotId, "clinicSlotId");
        slotSeq = ArgumentChecks.requireNonNull(slotSeq, "slotSeq");
        slotStartTime = ArgumentChecks.requireNonNull(slotStartTime, "slotStartTime");
        slotEndTime = ArgumentChecks.requireNonNull(slotEndTime, "slotEndTime");
    }
}
