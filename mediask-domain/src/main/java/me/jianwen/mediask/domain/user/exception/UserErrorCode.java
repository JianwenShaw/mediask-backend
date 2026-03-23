package me.jianwen.mediask.domain.user.exception;

import me.jianwen.mediask.common.exception.ErrorCodeCategory;
import me.jianwen.mediask.common.exception.ErrorCodeType;

public enum UserErrorCode implements ErrorCodeType {
    INVALID_CREDENTIALS(2001, "invalid username or password", ErrorCodeCategory.UNAUTHORIZED),
    ACCOUNT_DISABLED(2002, "account disabled", ErrorCodeCategory.FORBIDDEN),
    ACCOUNT_LOCKED(2003, "account locked", ErrorCodeCategory.FORBIDDEN),
    AUTHENTICATED_USER_NOT_FOUND(2004, "authenticated user not found", ErrorCodeCategory.UNAUTHORIZED),
    ROLE_NOT_ASSIGNED(2005, "role not assigned", ErrorCodeCategory.FORBIDDEN);

    private final int code;
    private final String message;
    private final ErrorCodeCategory category;

    UserErrorCode(int code, String message, ErrorCodeCategory category) {
        this.code = code;
        this.message = message;
        this.category = category;
    }

    @Override
    public int getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return message;
    }

    @Override
    public ErrorCodeCategory getCategory() {
        return category;
    }
}
