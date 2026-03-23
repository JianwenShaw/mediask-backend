package me.jianwen.mediask.domain.user.port;

import me.jianwen.mediask.domain.user.model.AuthenticatedUser;

public interface AccessTokenCodec {

    String issueAccessToken(AuthenticatedUser authenticatedUser);

    AuthenticatedUser parseAccessToken(String accessToken);
}
