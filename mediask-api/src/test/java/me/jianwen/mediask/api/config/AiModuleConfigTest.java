package me.jianwen.mediask.api.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.function.Consumer;
import me.jianwen.mediask.api.controller.AiController;
import me.jianwen.mediask.domain.ai.model.AiChatReply;
import me.jianwen.mediask.domain.ai.model.AiChatInvocation;
import me.jianwen.mediask.domain.ai.model.AiChatStreamEvent;
import me.jianwen.mediask.domain.ai.port.AiChatPort;
import me.jianwen.mediask.domain.ai.port.AiContentEncryptorPort;
import me.jianwen.mediask.domain.ai.port.AiGuardrailEventRepository;
import me.jianwen.mediask.domain.ai.port.AiModelRunRepository;
import me.jianwen.mediask.domain.ai.port.AiSessionRepository;
import me.jianwen.mediask.domain.ai.port.AiChatStreamPort;
import me.jianwen.mediask.domain.ai.port.AiTurnContentRepository;
import me.jianwen.mediask.domain.ai.port.AiTurnRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

class AiModuleConfigTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(AiModuleConfig.class, AiController.class, TestAiStreamConfig.class);

    @Test
    void contextWithoutAiServiceProperties_ShouldNotCreateStreamBeans() {
        contextRunner.run(context -> {
            assertThat(context).doesNotHaveBean(AiController.class);
            assertThat(context).doesNotHaveBean("chatAiUseCase");
            assertThat(context).doesNotHaveBean("streamAiChatUseCase");
            assertThat(context).doesNotHaveBean("aiSseTaskExecutor");
        });
    }

    @Test
    void contextWithAiServiceProperties_ShouldCreateStreamBeans() {
        contextRunner
                .withPropertyValues(
                        "mediask.ai.service.base-url=http://localhost:8000",
                        "mediask.ai.service.api-key=test-key")
                .run(context -> {
                    assertThat(context).hasSingleBean(AiController.class);
                    assertThat(context).hasBean("chatAiUseCase");
                    assertThat(context).hasBean("streamAiChatUseCase");
                    assertThat(context).hasBean("aiSseTaskExecutor");
                });
    }

    @Configuration
    static class TestAiStreamConfig {

        @Bean
        AiChatStreamPort aiChatStreamPort() {
            return new NoopAiChatStreamPort();
        }

        @Bean
        AiChatPort aiChatPort() {
            return invocation -> new AiChatReply(
                    "answer",
                    null,
                    me.jianwen.mediask.domain.ai.model.RiskLevel.LOW,
                    me.jianwen.mediask.domain.ai.model.GuardrailAction.ALLOW,
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
            return plainText -> plainText;
        }
    }

    static class NoopAiChatStreamPort implements AiChatStreamPort {

        @Override
        public void stream(AiChatInvocation invocation, Consumer<AiChatStreamEvent> eventConsumer) {}
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
    }
}
