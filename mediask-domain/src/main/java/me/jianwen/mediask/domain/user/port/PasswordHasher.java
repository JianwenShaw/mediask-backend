package me.jianwen.mediask.domain.user.port;

public interface PasswordHasher {

    String hash(CharSequence rawPassword);
}
