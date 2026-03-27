package me.jianwen.mediask.application.user.command;

import me.jianwen.mediask.common.util.ArgumentChecks;

public record LogoutCommand(String refreshToken, String accessToken, Long authenticatedUserId) {

    public LogoutCommand {
        refreshToken = ArgumentChecks.requireNonBlank(refreshToken, "refreshToken");
        accessToken = ArgumentChecks.blankToNull(accessToken);
        authenticatedUserId = ArgumentChecks.normalizePositive(authenticatedUserId, "authenticatedUserId");
    }
}
