package me.jianwen.mediask.application.authz;

import java.util.Locale;

public enum ResourceType {
    EMR_RECORD,
    AI_SESSION;

    public String code() {
        return name().toUpperCase(Locale.ROOT);
    }
}
