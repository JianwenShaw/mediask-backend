package me.jianwen.mediask.infra.persistence.mapper;

import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ClinicSessionSlotListRow {

    private Long clinicSlotId;
    private Integer slotSeq;
    private OffsetDateTime slotStartTime;
    private OffsetDateTime slotEndTime;
}
