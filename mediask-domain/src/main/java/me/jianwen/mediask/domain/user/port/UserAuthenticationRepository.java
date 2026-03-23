package me.jianwen.mediask.domain.user.port;

import java.util.Optional;
import me.jianwen.mediask.domain.user.model.AuthenticatedUser;
import me.jianwen.mediask.domain.user.model.LoginAccount;

public interface UserAuthenticationRepository {

    Optional<LoginAccount> findLoginAccountByUsername(String username);

    Optional<AuthenticatedUser> findAuthenticatedUserById(Long userId);

    void updateLastLoginAt(Long userId);
}
