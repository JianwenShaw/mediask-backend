package me.jianwen.mediask.common.util;

import java.util.UUID;

public final class RequestIdUtils {

    private RequestIdUtils() {
    }

    public static String generate() {
        return "req_" + UUID.randomUUID().toString().replace("-", "");
    }
}
