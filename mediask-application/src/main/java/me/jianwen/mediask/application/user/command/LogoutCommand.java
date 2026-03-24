package me.jianwen.mediask.application.user.command;

public record LogoutCommand(String refreshToken, String accessToken, Long authenticatedUserId) {

    public LogoutCommand {
        refreshToken = requireNonBlank(refreshToken, "refreshToken").trim();
        accessToken = normalizeNullable(accessToken);
        authenticatedUserId = normalizeNullablePositive(authenticatedUserId, "authenticatedUserId");
    }

    private static String requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }

    private static String normalizeNullable(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static Long normalizeNullablePositive(Long value, String fieldName) {
        if (value == null) {
            return null;
        }
        if (value <= 0L) {
            throw new IllegalArgumentException(fieldName + " must be greater than 0");
        }
        return value;
    }
}
