package me.jianwen.mediask.application.user.command;

public record RefreshTokenCommand(String refreshToken) {

    public RefreshTokenCommand {
        refreshToken = requireNonBlank(refreshToken, "refreshToken").trim();
    }

    private static String requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }
}
