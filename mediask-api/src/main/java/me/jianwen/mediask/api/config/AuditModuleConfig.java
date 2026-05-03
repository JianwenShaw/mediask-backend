package me.jianwen.mediask.api.config;

import me.jianwen.mediask.application.audit.usecase.AuditTrailService;
import me.jianwen.mediask.application.audit.usecase.QueryAuditEventsUseCase;
import me.jianwen.mediask.application.audit.usecase.QueryDataAccessLogsUseCase;
import me.jianwen.mediask.application.audit.usecase.RecordAuditEventUseCase;
import me.jianwen.mediask.application.audit.usecase.RecordDataAccessLogUseCase;
import me.jianwen.mediask.domain.audit.port.AuditEventRepository;
import me.jianwen.mediask.domain.audit.port.AuditQueryRepository;
import me.jianwen.mediask.domain.audit.port.DataAccessLogRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AuditModuleConfig {

    @Bean
    public RecordAuditEventUseCase recordAuditEventUseCase(AuditEventRepository auditEventRepository) {
        return new RecordAuditEventUseCase(auditEventRepository);
    }

    @Bean
    public RecordDataAccessLogUseCase recordDataAccessLogUseCase(DataAccessLogRepository dataAccessLogRepository) {
        return new RecordDataAccessLogUseCase(dataAccessLogRepository);
    }

    @Bean
    public AuditTrailService auditTrailService(
            RecordAuditEventUseCase recordAuditEventUseCase,
            RecordDataAccessLogUseCase recordDataAccessLogUseCase) {
        return new AuditTrailService(recordAuditEventUseCase, recordDataAccessLogUseCase);
    }

    @Bean
    public QueryAuditEventsUseCase queryAuditEventsUseCase(AuditQueryRepository auditQueryRepository) {
        return new QueryAuditEventsUseCase(auditQueryRepository);
    }

    @Bean
    public QueryDataAccessLogsUseCase queryDataAccessLogsUseCase(AuditQueryRepository auditQueryRepository) {
        return new QueryDataAccessLogsUseCase(auditQueryRepository);
    }
}
