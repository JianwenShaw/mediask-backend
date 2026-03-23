package me.jianwen.mediask.api.dto;

public record LoginRequest(String username, String password) {

    public LoginRequest {
        username = normalizeUsername(username);
        password = preservePassword(password);
    }

    private static String normalizeUsername(String value) {
        return requireNonBlank(value, "username").trim();
    }

    private static String preservePassword(String value) {
        return requireNonBlank(value, "password");
    }

    private static String requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }
}
