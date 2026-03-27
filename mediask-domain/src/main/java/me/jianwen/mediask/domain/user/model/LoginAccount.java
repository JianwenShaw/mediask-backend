package me.jianwen.mediask.domain.user.model;

import java.util.Objects;
import me.jianwen.mediask.common.util.ArgumentChecks;

public record LoginAccount(AuthenticatedUser authenticatedUser, String passwordHash, AccountStatus accountStatus) {

    public LoginAccount {
        authenticatedUser = Objects.requireNonNull(authenticatedUser, "authenticatedUser must not be null");
        passwordHash = ArgumentChecks.requireNonBlank(passwordHash, "passwordHash");
        accountStatus = Objects.requireNonNull(accountStatus, "accountStatus must not be null");
    }
}
