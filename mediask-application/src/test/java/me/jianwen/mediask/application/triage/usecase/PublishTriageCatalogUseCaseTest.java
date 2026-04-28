package me.jianwen.mediask.application.triage.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import me.jianwen.mediask.application.triage.command.PublishCatalogCommand;
import me.jianwen.mediask.application.triage.result.PublishCatalogResult;
import me.jianwen.mediask.common.exception.BizException;
import me.jianwen.mediask.domain.triage.exception.TriageErrorCode;
import me.jianwen.mediask.domain.triage.model.CatalogVersion;
import me.jianwen.mediask.domain.triage.model.DepartmentCandidate;
import me.jianwen.mediask.domain.triage.model.TriageCatalog;
import me.jianwen.mediask.domain.triage.port.DepartmentCatalogSourcePort;
import me.jianwen.mediask.domain.triage.port.TriageCatalogPublishPort;
import org.junit.jupiter.api.Test;

class PublishTriageCatalogUseCaseTest {

    @Test
    void handle_WhenCandidatesExist_PublishCatalog() {
        StubPublishPort publishPort = new StubPublishPort();
        StubSourcePort sourcePort = new StubSourcePort();
        PublishTriageCatalogUseCase useCase = new PublishTriageCatalogUseCase(publishPort, sourcePort);

        PublishCatalogResult result = useCase.handle(new PublishCatalogCommand("default"));

        assertTrue(publishPort.published);
        assertEquals("deptcat-v" + LocalDate.now().format(java.time.format.DateTimeFormatter.BASIC_ISO_DATE) + "-01",
                result.catalogVersion());
        assertEquals(2, result.candidateCount());
    }

    @Test
    void handle_WhenNoCandidatesConfigured_ThrowNoDepartmentsConfigured() {
        StubPublishPort publishPort = new StubPublishPort();
        StubSourcePort sourcePort = new StubSourcePort();
        sourcePort.candidates = List.of();
        PublishTriageCatalogUseCase useCase = new PublishTriageCatalogUseCase(publishPort, sourcePort);

        BizException exception = assertThrows(BizException.class,
                () -> useCase.handle(new PublishCatalogCommand("default")));
        assertEquals(TriageErrorCode.NO_DEPARTMENTS_CONFIGURED, exception.getErrorCode());
    }

    private static class StubPublishPort implements TriageCatalogPublishPort {

        boolean published = false;

        @Override
        public void publish(TriageCatalog catalog) {
            published = true;
        }

        @Override
        public Optional<TriageCatalog> findActiveCatalog(String hospitalScope) {
            return Optional.empty();
        }

        @Override
        public Optional<TriageCatalog> findCatalogByVersion(String hospitalScope, CatalogVersion version) {
            return Optional.empty();
        }

        @Override
        public Optional<CatalogVersion> findActiveVersion(String hospitalScope) {
            return Optional.empty();
        }

        @Override
        public CatalogVersion nextVersion(String hospitalScope) {
            return CatalogVersion.of(LocalDate.now(), 1);
        }
    }

    private static class StubSourcePort implements DepartmentCatalogSourcePort {

        List<DepartmentCandidate> candidates = List.of(
                new DepartmentCandidate(101L, "神经内科", "头痛头晕", List.of("神内"), 10),
                new DepartmentCandidate(102L, "心内科", "胸闷胸痛", List.of(), 20));

        @Override
        public List<DepartmentCandidate> loadCandidates(String hospitalScope) {
            return candidates;
        }
    }
}
