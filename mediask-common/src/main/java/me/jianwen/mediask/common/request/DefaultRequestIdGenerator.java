package me.jianwen.mediask.common.request;

import java.util.UUID;

public final class DefaultRequestIdGenerator implements RequestIdGenerator {

    public static final DefaultRequestIdGenerator INSTANCE = new DefaultRequestIdGenerator();

    private DefaultRequestIdGenerator() {
    }

    @Override
    public String generate() {
        return "req_" + UUID.randomUUID().toString().replace("-", "");
    }
}
