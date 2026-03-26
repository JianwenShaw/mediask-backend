package me.jianwen.mediask.application.authz;

import java.util.Objects;

public record AuthzDecision(boolean allowed, AuthzDecisionReason reason) {

    public AuthzDecision {
        reason = Objects.requireNonNull(reason, "reason must not be null");
    }

    public static AuthzDecision allow() {
        return new AuthzDecision(true, AuthzDecisionReason.ALLOWED);
    }

    public static AuthzDecision deny(AuthzDecisionReason reason) {
        return new AuthzDecision(false, reason);
    }
}
