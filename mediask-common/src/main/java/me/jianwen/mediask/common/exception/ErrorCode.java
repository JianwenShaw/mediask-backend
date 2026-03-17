package me.jianwen.mediask.common.exception;

public enum ErrorCode implements ErrorCodeType {
    // 0: success
    SUCCESS(0, "success", ErrorCodeCategory.SUCCESS),

    // 1xxx: common
    UNAUTHORIZED(1001, "unauthorized", ErrorCodeCategory.UNAUTHORIZED),
    INVALID_PARAMETER(1002, "invalid parameter", ErrorCodeCategory.BAD_REQUEST),
    FORBIDDEN(1003, "forbidden", ErrorCodeCategory.FORBIDDEN),
    RESOURCE_NOT_FOUND(1004, "resource not found", ErrorCodeCategory.NOT_FOUND),
    INVALID_PHONE_NUMBER(1005, "invalid phone number", ErrorCodeCategory.BAD_REQUEST),

    // 9xxx: system
    SYSTEM_ERROR(9999, "system error", ErrorCodeCategory.INTERNAL_ERROR);

    private final int code;
    private final String message;
    private final ErrorCodeCategory category;

    ErrorCode(int code, String message, ErrorCodeCategory category) {
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
