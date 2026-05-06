package me.jianwen.mediask.application.user.command;

public record LoginCommand(String phone, String password) {

    public LoginCommand {
        phone = normalizePhone(phone);
        password = preservePassword(password);
    }

    private static String normalizePhone(String value) {
        return requireNonBlank(value, "phone").trim();
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
