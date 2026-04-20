package me.jianwen.mediask.api.dto;

public record ClinicSessionSlotListItemResponse(
        Long clinicSlotId, Integer slotSeq, String slotStartTime, String slotEndTime) {
}
