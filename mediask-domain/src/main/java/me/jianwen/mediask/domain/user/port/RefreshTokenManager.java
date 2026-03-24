package me.jianwen.mediask.domain.user.port;

import me.jianwen.mediask.domain.user.model.RefreshTokenSession;

public interface RefreshTokenManager {

    RefreshTokenSession issue(Long userId);
}
