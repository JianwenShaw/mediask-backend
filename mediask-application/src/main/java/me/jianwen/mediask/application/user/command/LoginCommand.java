package me.jianwen.mediask.application.user.command;

public record LoginCommand(String username, String password) {

    public LoginCommand {
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
