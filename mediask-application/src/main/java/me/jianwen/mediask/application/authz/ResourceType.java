package me.jianwen.mediask.application.authz;

import java.util.Locale;

public enum ResourceType {
    EMR_RECORD,
    PRESCRIPTION_ORDER;

    public String code() {
        return name().toUpperCase(Locale.ROOT);
    }
}
