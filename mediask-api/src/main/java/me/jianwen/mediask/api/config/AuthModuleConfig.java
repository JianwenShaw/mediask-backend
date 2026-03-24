package me.jianwen.mediask.api.config;

import java.time.Clock;
import me.jianwen.mediask.application.user.usecase.GetCurrentUserUseCase;
import me.jianwen.mediask.application.user.usecase.GetCurrentDoctorProfileUseCase;
import me.jianwen.mediask.application.user.usecase.GetCurrentPatientProfileUseCase;
import me.jianwen.mediask.application.user.usecase.LoginUseCase;
import me.jianwen.mediask.application.user.usecase.LogoutUseCase;
import me.jianwen.mediask.application.user.usecase.RefreshTokenUseCase;
import me.jianwen.mediask.domain.user.port.AccessTokenCodec;
import me.jianwen.mediask.domain.user.port.AccessTokenBlocklistPort;
import me.jianwen.mediask.domain.user.port.DoctorProfileRepository;
import me.jianwen.mediask.domain.user.port.PasswordVerifier;
import me.jianwen.mediask.domain.user.port.PatientProfileRepository;
import me.jianwen.mediask.domain.user.port.RefreshTokenManager;
import me.jianwen.mediask.domain.user.port.RefreshTokenStore;
import me.jianwen.mediask.domain.user.port.UserAuthenticationRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AuthModuleConfig {

    @Bean
    public LoginUseCase loginUseCase(
            UserAuthenticationRepository userAuthenticationRepository,
            PasswordVerifier passwordVerifier,
            AccessTokenCodec accessTokenCodec,
            RefreshTokenManager refreshTokenManager,
            RefreshTokenStore refreshTokenStore) {
        return new LoginUseCase(
                userAuthenticationRepository,
                passwordVerifier,
                accessTokenCodec,
                refreshTokenManager,
                refreshTokenStore);
    }

    @Bean
    public GetCurrentUserUseCase getCurrentUserUseCase(UserAuthenticationRepository userAuthenticationRepository) {
        return new GetCurrentUserUseCase(userAuthenticationRepository);
    }

    @Bean
    public RefreshTokenUseCase refreshTokenUseCase(
            UserAuthenticationRepository userAuthenticationRepository,
            AccessTokenCodec accessTokenCodec,
            RefreshTokenManager refreshTokenManager,
            RefreshTokenStore refreshTokenStore,
            Clock authClock) {
        return new RefreshTokenUseCase(
                userAuthenticationRepository, accessTokenCodec, refreshTokenManager, refreshTokenStore, authClock);
    }

    @Bean
    public LogoutUseCase logoutUseCase(
            RefreshTokenStore refreshTokenStore,
            AccessTokenBlocklistPort accessTokenBlocklistPort,
            AccessTokenCodec accessTokenCodec,
            Clock authClock) {
        return new LogoutUseCase(refreshTokenStore, accessTokenBlocklistPort, accessTokenCodec, authClock);
    }

    @Bean
    public GetCurrentPatientProfileUseCase getCurrentPatientProfileUseCase(
            UserAuthenticationRepository userAuthenticationRepository, PatientProfileRepository patientProfileRepository) {
        return new GetCurrentPatientProfileUseCase(userAuthenticationRepository, patientProfileRepository);
    }

    @Bean
    public GetCurrentDoctorProfileUseCase getCurrentDoctorProfileUseCase(
            UserAuthenticationRepository userAuthenticationRepository, DoctorProfileRepository doctorProfileRepository) {
        return new GetCurrentDoctorProfileUseCase(userAuthenticationRepository, doctorProfileRepository);
    }
}
