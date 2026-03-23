package me.jianwen.mediask.domain.user.model;

import java.util.Locale;

public enum RoleCode {
    PATIENT,
    DOCTOR,
    ADMIN;

    public static RoleCode fromCode(String code) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("roleCode must not be blank");
        }
        return RoleCode.valueOf(code.trim().toUpperCase(Locale.ROOT));
    }
}
