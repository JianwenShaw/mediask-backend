package me.jianwen.mediask.domain.user.port;

import me.jianwen.mediask.domain.user.model.AuthenticatedUser;
import me.jianwen.mediask.domain.user.model.AccessToken;
import me.jianwen.mediask.domain.user.model.AccessTokenClaims;

public interface AccessTokenCodec {

    AccessToken issueAccessToken(AuthenticatedUser authenticatedUser);

    AccessTokenClaims parseAccessToken(String accessToken);
}
