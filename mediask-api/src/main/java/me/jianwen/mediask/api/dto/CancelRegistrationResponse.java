package me.jianwen.mediask.api.dto;

public record CancelRegistrationResponse(Long registrationId, String status, String cancelledAt) {
}
