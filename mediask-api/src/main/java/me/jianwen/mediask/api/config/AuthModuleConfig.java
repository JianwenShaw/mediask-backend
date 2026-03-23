package me.jianwen.mediask.api.config;

import me.jianwen.mediask.application.user.usecase.GetCurrentUserUseCase;
import me.jianwen.mediask.application.user.usecase.LoginUseCase;
import me.jianwen.mediask.domain.user.port.AccessTokenCodec;
import me.jianwen.mediask.domain.user.port.PasswordVerifier;
import me.jianwen.mediask.domain.user.port.UserAuthenticationRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AuthModuleConfig {

    @Bean
    public LoginUseCase loginUseCase(
            UserAuthenticationRepository userAuthenticationRepository,
            PasswordVerifier passwordVerifier,
            AccessTokenCodec accessTokenCodec) {
        return new LoginUseCase(userAuthenticationRepository, passwordVerifier, accessTokenCodec);
    }

    @Bean
    public GetCurrentUserUseCase getCurrentUserUseCase(UserAuthenticationRepository userAuthenticationRepository) {
        return new GetCurrentUserUseCase(userAuthenticationRepository);
    }
}
