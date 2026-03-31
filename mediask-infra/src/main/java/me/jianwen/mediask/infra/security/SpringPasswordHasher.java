package me.jianwen.mediask.infra.security;

import me.jianwen.mediask.domain.user.port.PasswordHasher;
import org.springframework.security.crypto.password.PasswordEncoder;

public final class SpringPasswordHasher implements PasswordHasher {

    private final PasswordEncoder passwordEncoder;

    public SpringPasswordHasher(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public String hash(CharSequence rawPassword) {
        if (rawPassword == null) {
            throw new IllegalArgumentException("rawPassword must not be null");
        }
        return passwordEncoder.encode(rawPassword.toString());
    }
}
