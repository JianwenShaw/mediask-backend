package me.jianwen.mediask.domain.user.model;

import java.util.Locale;

public enum AccountStatus {
    ACTIVE,
    DISABLED,
    LOCKED;

    public static AccountStatus fromCode(String code) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("accountStatus must not be blank");
        }
        return AccountStatus.valueOf(code.trim().toUpperCase(Locale.ROOT));
    }
}
