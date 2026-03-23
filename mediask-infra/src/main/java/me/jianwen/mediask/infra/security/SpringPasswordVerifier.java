package me.jianwen.mediask.infra.security;

import me.jianwen.mediask.domain.user.port.PasswordVerifier;
import org.springframework.security.crypto.password.PasswordEncoder;

public final class SpringPasswordVerifier implements PasswordVerifier {

    private final PasswordEncoder passwordEncoder;

    public SpringPasswordVerifier(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public boolean matches(CharSequence rawPassword, String encodedPassword) {
        if (rawPassword == null || encodedPassword == null || encodedPassword.isBlank()) {
            return false;
        }
        return passwordEncoder.matches(rawPassword.toString(), encodedPassword);
    }
}
