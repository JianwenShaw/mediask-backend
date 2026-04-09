package me.jianwen.mediask.api.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.function.Consumer;
import me.jianwen.mediask.api.controller.AiController;
import me.jianwen.mediask.domain.ai.model.AiChatInvocation;
import me.jianwen.mediask.domain.ai.model.AiChatStreamEvent;
import me.jianwen.mediask.domain.ai.port.AiChatStreamPort;
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
    }

    static class NoopAiChatStreamPort implements AiChatStreamPort {

        @Override
        public void stream(AiChatInvocation invocation, Consumer<AiChatStreamEvent> eventConsumer) {}
    }
}
