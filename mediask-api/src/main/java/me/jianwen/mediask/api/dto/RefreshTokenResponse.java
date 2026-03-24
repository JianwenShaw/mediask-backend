package me.jianwen.mediask.api.dto;

public record RefreshTokenResponse(
        String accessToken,
        long accessTokenExpiresAt,
        String refreshToken,
        long refreshTokenExpiresAt,
        CurrentUserResponse userContext) {
}
