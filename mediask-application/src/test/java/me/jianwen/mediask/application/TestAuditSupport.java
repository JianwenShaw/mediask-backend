package me.jianwen.mediask.application;

import java.time.OffsetDateTime;
import me.jianwen.mediask.application.audit.model.AuditContext;
import me.jianwen.mediask.application.audit.usecase.AuditTrailService;
import me.jianwen.mediask.application.audit.usecase.RecordAuditEventUseCase;
import me.jianwen.mediask.application.audit.usecase.RecordDataAccessLogUseCase;
import me.jianwen.mediask.domain.audit.model.AuditActorType;

public final class TestAuditSupport {

    private TestAuditSupport() {
    }

    public static AuditTrailService auditTrailService() {
        return new AuditTrailService(
                new RecordAuditEventUseCase(record -> {}),
                new RecordDataAccessLogUseCase(record -> {}));
    }

    public static AuditContext auditContext() {
        return new AuditContext(
                "req-test",
                null,
                AuditActorType.USER,
                2001L,
                "test-user",
                "TEST",
                3101L,
                "127.0.0.1",
                "JUnit",
                OffsetDateTime.parse("2026-05-01T09:00:00Z"));
    }
}
