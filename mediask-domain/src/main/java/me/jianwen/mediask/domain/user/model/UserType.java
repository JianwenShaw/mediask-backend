package me.jianwen.mediask.domain.user.model;

import java.util.Locale;

public enum UserType {
    PATIENT,
    DOCTOR,
    ADMIN;

    public static UserType fromCode(String code) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("userType must not be blank");
        }
        return UserType.valueOf(code.trim().toUpperCase(Locale.ROOT));
    }
}
