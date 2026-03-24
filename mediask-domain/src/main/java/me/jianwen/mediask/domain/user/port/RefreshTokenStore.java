package me.jianwen.mediask.domain.user.port;

import java.util.Optional;
import me.jianwen.mediask.domain.user.model.RefreshTokenSession;

public interface RefreshTokenStore {

    void save(RefreshTokenSession refreshTokenSession);

    boolean rotate(String currentRefreshToken, RefreshTokenSession nextRefreshTokenSession);

    Optional<RefreshTokenSession> findByTokenValue(String refreshToken);

    void deleteByTokenValue(String refreshToken);
}
