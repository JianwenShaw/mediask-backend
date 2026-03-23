package me.jianwen.mediask.infra.security;

import java.time.Clock;
import me.jianwen.mediask.domain.user.port.AccessTokenCodec;
import me.jianwen.mediask.domain.user.port.PasswordVerifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@ConditionalOnProperty(prefix = "mediask.jwt", name = "secret")
@EnableConfigurationProperties(JwtProperties.class)
public class UserAuthInfrastructureConfig {

    @Bean
    public Clock authClock() {
        return Clock.systemUTC();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public PasswordVerifier passwordVerifier(PasswordEncoder passwordEncoder) {
        return new SpringPasswordVerifier(passwordEncoder);
    }

    @Bean
    public AccessTokenCodec accessTokenCodec(JwtProperties jwtProperties, Clock authClock) {
        return new JwtAccessTokenCodec(jwtProperties, authClock);
    }
}
