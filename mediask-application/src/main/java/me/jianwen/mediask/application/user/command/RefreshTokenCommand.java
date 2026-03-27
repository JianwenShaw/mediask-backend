package me.jianwen.mediask.application.user.command;

import me.jianwen.mediask.common.util.ArgumentChecks;

public record RefreshTokenCommand(String refreshToken) {

    public RefreshTokenCommand {
        refreshToken = ArgumentChecks.requireNonBlank(refreshToken, "refreshToken");
    }
}
