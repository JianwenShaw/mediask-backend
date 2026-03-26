package me.jianwen.mediask.domain.user.model;

import java.util.Locale;

public enum DataScopeType {
    SELF,
    DEPARTMENT,
    ALL,
    CUSTOM;

    public static DataScopeType fromCode(String code) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("dataScopeType must not be blank");
        }
        return DataScopeType.valueOf(code.trim().toUpperCase(Locale.ROOT));
    }
}
