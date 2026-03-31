package me.jianwen.mediask.infra.security;

import java.time.Clock;
import me.jianwen.mediask.domain.user.port.AccessTokenCodec;
import me.jianwen.mediask.domain.user.port.AccessTokenBlocklistPort;
import me.jianwen.mediask.domain.user.port.PasswordHasher;
import me.jianwen.mediask.domain.user.port.PasswordVerifier;
import me.jianwen.mediask.domain.user.port.RefreshTokenManager;
import me.jianwen.mediask.domain.user.port.RefreshTokenStore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
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
    public PasswordHasher passwordHasher(PasswordEncoder passwordEncoder) {
        return new SpringPasswordHasher(passwordEncoder);
    }

    @Bean
    public AccessTokenCodec accessTokenCodec(JwtProperties jwtProperties, Clock authClock) {
        return new JwtAccessTokenCodec(jwtProperties, authClock);
    }

    @Bean
    public RefreshTokenManager refreshTokenManager(JwtProperties jwtProperties, Clock authClock) {
        return new DefaultRefreshTokenManager(jwtProperties, authClock);
    }

    @Bean
    public RefreshTokenStore refreshTokenStore(StringRedisTemplate stringRedisTemplate, Clock authClock) {
        return new RedisRefreshTokenStore(stringRedisTemplate, authClock);
    }

    @Bean
    public AccessTokenBlocklistPort accessTokenBlocklistPort(
            StringRedisTemplate stringRedisTemplate, Clock authClock) {
        return new RedisAccessTokenBlocklistPort(stringRedisTemplate, authClock);
    }
}
