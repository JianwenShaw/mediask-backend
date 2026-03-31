package me.jianwen.mediask.api.dto;

public record LoginResponse(
        String accessToken,
        long accessTokenExpiresAt,
        String refreshToken,
        long refreshTokenExpiresAt,
        CurrentUserResponse userContext) {
}
