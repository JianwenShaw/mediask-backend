package me.jianwen.mediask.application.triage.result;

import java.time.OffsetDateTime;

public record PublishCatalogResult(
        String catalogVersion,
        int candidateCount,
        OffsetDateTime publishedAt) {
}
