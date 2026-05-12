package me.jianwen.mediask.worker.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.jianwen.mediask.application.triage.command.PublishCatalogCommand;
import me.jianwen.mediask.application.triage.result.PublishCatalogResult;
import me.jianwen.mediask.application.triage.usecase.PublishTriageCatalogUseCase;
import me.jianwen.mediask.common.exception.BizException;
import me.jianwen.mediask.domain.triage.exception.TriageErrorCode;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TriageCatalogPublishScheduler {

    private static final String DEFAULT_HOSPITAL_SCOPE = "default";

    private final PublishTriageCatalogUseCase publishTriageCatalogUseCase;

    @Scheduled(cron = "0 0 4 * * ?")
    public void publishTriageCatalog() {
        log.info("Scheduled triage catalog publish started for scope: {}", DEFAULT_HOSPITAL_SCOPE);
        try {
            PublishCatalogResult result = publishTriageCatalogUseCase.handle(
                    new PublishCatalogCommand(DEFAULT_HOSPITAL_SCOPE));
            log.info("Scheduled triage catalog publish completed. version={}, candidateCount={}, publishedAt={}",
                    result.catalogVersion(), result.candidateCount(), result.publishedAt());
        } catch (BizException e) {
            if (e.getErrorCode() == TriageErrorCode.NO_DEPARTMENTS_CONFIGURED) {
                log.warn("Scheduled triage catalog publish skipped: no departments configured for scope '{}'",
                        DEFAULT_HOSPITAL_SCOPE);
            } else {
                log.error("Scheduled triage catalog publish failed: {} (code={})", e.getMessage(), e.getCode(), e);
            }
        } catch (Exception e) {
            log.error("Unexpected error during scheduled triage catalog publish", e);
        }
    }
}
