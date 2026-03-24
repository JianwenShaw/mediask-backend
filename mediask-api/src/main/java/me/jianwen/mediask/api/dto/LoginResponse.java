package me.jianwen.mediask.api.dto;

import java.util.List;

public record LoginResponse(
        String accessToken,
        long accessTokenExpiresAt,
        String refreshToken,
        long refreshTokenExpiresAt,
        CurrentUserResponse userContext) {
}
