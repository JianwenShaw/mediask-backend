package me.jianwen.mediask.infra.triage.cache;

import java.time.Duration;

public final class TriageCatalogCachePolicy {

    public static final Duration SEQUENCE_COUNTER_TTL = Duration.ofHours(25);

    private TriageCatalogCachePolicy() {}
}
