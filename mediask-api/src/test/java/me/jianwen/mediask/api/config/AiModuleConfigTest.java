package me.jianwen.mediask.api.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.time.Duration;
import me.jianwen.mediask.api.controller.AiController;
import me.jianwen.mediask.api.controller.InternalTriageDepartmentCatalogController;
import me.jianwen.mediask.domain.ai.model.AiChatReply;
import me.jianwen.mediask.domain.ai.model.AiChatInvocation;
import me.jianwen.mediask.domain.ai.model.AiTriageCompletionReason;
import me.jianwen.mediask.domain.ai.model.AiTriageStage;
import me.jianwen.mediask.domain.ai.model.TriageDepartmentCatalog;
import me.jianwen.mediask.domain.ai.port.AiChatPort;
import me.jianwen.mediask.domain.ai.port.AiContentEncryptorPort;
import me.jianwen.mediask.domain.ai.port.AiGuardrailEventRepository;
import me.jianwen.mediask.domain.ai.port.AiModelRunRepository;
import me.jianwen.mediask.domain.ai.port.AiSessionQueryRepository;
import me.jianwen.mediask.domain.ai.port.AiSessionRepository;
import me.jianwen.mediask.domain.ai.port.AiTurnContentRepository;
import me.jianwen.mediask.domain.ai.port.AiTurnRepository;
import me.jianwen.mediask.domain.ai.port.TriageDepartmentCatalogPort;
import me.jianwen.mediask.infra.ai.config.AiServiceProperties;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

class AiModuleConfigTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(AiModuleConfig.class, AiController.class, InternalTriageDepartmentCatalogController.class, TestAiConfig.class);

    @Test
    void contextWithoutAiServiceProperties_ShouldNotCreateAiBeans() {
        contextRunner.run(context -> {
            assertThat(context).doesNotHaveBean(AiController.class);
            assertThat(context).doesNotHaveBean(InternalTriageDepartmentCatalogController.class);
            assertThat(context).doesNotHaveBean("chatAiUseCase");
        });
    }

    @Test
    void contextWithAiServiceProperties_ShouldCreateAiBeans() {
        contextRunner
                .withPropertyValues(
                        "mediask.ai.service.base-url=http://localhost:8000",
                        "mediask.ai.service.api-key=test-key")
                .run(context -> {
                    assertThat(context).hasSingleBean(AiController.class);
                    assertThat(context).hasSingleBean(InternalTriageDepartmentCatalogController.class);
                    assertThat(context).hasBean("chatAiUseCase");
                    assertThat(context).hasBean("listAiSessionsUseCase");
                });
    }

    @Configuration
    static class TestAiConfig {

        @Bean
        AiChatPort aiChatPort() {
            return invocation -> new AiChatReply(
                    "answer",
                    AiTriageStage.READY,
                    AiTriageCompletionReason.SUFFICIENT_INFO,
                    "summary",
                    me.jianwen.mediask.domain.ai.model.RiskLevel.LOW,
                    me.jianwen.mediask.domain.ai.model.GuardrailAction.ALLOW,
                    java.util.List.of(),
                    java.util.List.of(),
                    null,
                    java.util.List.of(),
                    me.jianwen.mediask.domain.ai.model.AiExecutionMetadata.empty());
        }

        @Bean
        AiSessionRepository aiSessionRepository() {
            return new NoopAiSessionRepository();
        }

        @Bean
        AiTurnRepository aiTurnRepository() {
            return new NoopAiTurnRepository();
        }

        @Bean
        AiTurnContentRepository aiTurnContentRepository() {
            return content -> {};
        }

        @Bean
        AiModelRunRepository aiModelRunRepository() {
            return new NoopAiModelRunRepository();
        }

        @Bean
        AiGuardrailEventRepository aiGuardrailEventRepository() {
            return event -> {};
        }

        @Bean
        AiContentEncryptorPort aiContentEncryptorPort() {
            return new AiContentEncryptorPort() {
                @Override
                public String encrypt(String plainText) {
                    return plainText;
                }

                @Override
                public String decrypt(String encryptedText) {
                    return encryptedText;
                }
            };
        }

        @Bean
        AiSessionQueryRepository aiSessionQueryRepository() {
            return new NoopAiSessionQueryRepository();
        }

        @Bean
        TriageDepartmentCatalogPort triageDepartmentCatalogPort() {
            return hospitalScope -> new TriageDepartmentCatalog(hospitalScope, "deptcat-test", java.util.List.of());
        }

        @Bean
        AiServiceProperties aiServiceProperties() {
            return new AiServiceProperties(
                    URI.create("http://localhost:8000"),
                    "test-key",
                    Duration.ofSeconds(3),
                    Duration.ofSeconds(30),
                    Duration.ofMinutes(5));
        }
    }

    static class NoopAiSessionRepository implements AiSessionRepository {
        @Override
        public void save(me.jianwen.mediask.domain.ai.model.AiSession aiSession) {}

        @Override
        public java.util.Optional<me.jianwen.mediask.domain.ai.model.AiSession> findById(Long sessionId) {
            return java.util.Optional.empty();
        }

        @Override
        public void update(me.jianwen.mediask.domain.ai.model.AiSession aiSession) {}
    }

    static class NoopAiTurnRepository implements AiTurnRepository {
        @Override
        public void save(me.jianwen.mediask.domain.ai.model.AiTurn aiTurn) {}

        @Override
        public int findMaxTurnNoBySessionId(Long sessionId) {
            return 0;
        }

        @Override
        public void update(me.jianwen.mediask.domain.ai.model.AiTurn aiTurn) {}
    }

    static class NoopAiModelRunRepository implements AiModelRunRepository {
        @Override
        public void save(me.jianwen.mediask.domain.ai.model.AiModelRun aiModelRun) {}

        @Override
        public void update(me.jianwen.mediask.domain.ai.model.AiModelRun aiModelRun) {}

        @Override
        public Integer findLatestFinalizedTurnNoBySessionId(Long sessionId) {
            return null;
        }
    }

    static class NoopAiSessionQueryRepository implements AiSessionQueryRepository {
        @Override
        public java.util.List<me.jianwen.mediask.domain.ai.model.AiSessionListItem> listSessionsByPatientUserId(
                Long patientUserId) {
            return java.util.List.of();
        }

        @Override
        public java.util.Optional<me.jianwen.mediask.domain.ai.model.AiSessionDetail> findSessionDetailById(Long sessionId) {
            return java.util.Optional.empty();
        }

        @Override
        public java.util.Optional<me.jianwen.mediask.domain.ai.model.AiSessionTriageResultView> findLatestTriageResultBySessionId(
                Long sessionId) {
            return java.util.Optional.empty();
        }

        @Override
        public java.util.Optional<me.jianwen.mediask.domain.ai.model.AiTriageStage> findLatestTriageStageBySessionId(
                Long sessionId) {
            return java.util.Optional.empty();
        }

        @Override
        public boolean hasAccessibleTriageSession(Long patientUserId, Long sessionId) {
            return false;
        }
    }
}
