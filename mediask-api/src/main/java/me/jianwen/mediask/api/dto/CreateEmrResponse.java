package me.jianwen.mediask.api.dto;

import java.util.List;

public record CreateEmrResponse(
        Long recordId,
        String recordNo,
        Long encounterId,
        String recordStatus,
        int version) {
}