package me.jianwen.mediask.domain.user.port;

public interface PasswordVerifier {

    boolean matches(CharSequence rawPassword, String encodedPassword);
}
