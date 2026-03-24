package me.jianwen.mediask.domain.user.port;

import java.time.Instant;

public interface AccessTokenBlocklistPort {

    void block(String tokenId, Instant expiresAt);

    boolean isBlocked(String tokenId);
}
