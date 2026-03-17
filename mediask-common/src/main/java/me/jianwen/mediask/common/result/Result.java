package me.jianwen.mediask.common.result;

import me.jianwen.mediask.common.exception.BaseException;
import me.jianwen.mediask.common.exception.BizException;
import me.jianwen.mediask.common.exception.ErrorCode;
import me.jianwen.mediask.common.exception.ErrorCodeType;

public record Result<T>(int code, String msg, T data, String requestId, long timestamp) {

    public boolean isSuccess() {
        return code == ErrorCode.SUCCESS.getCode();
    }

    public Result<T> withRequestId(String requestId) {
        return new Result<>(code, msg, data, requestId, timestamp);
    }

    public static <T> Result<T> ok() {
        return ok(null);
    }

    public static <T> Result<T> ok(T data) {
        return new Result<>(
                ErrorCode.SUCCESS.getCode(),
                ErrorCode.SUCCESS.getMessage(),
                data,
                null,
                currentTimestamp());
    }

    public static <T> Result<T> fail(ErrorCodeType errorCode) {
        return fail(errorCode, errorCode.getMessage());
    }

    public static <T> Result<T> fail(int code, String message) {
        return new Result<>(
                code,
                message,
                null,
                null,
                currentTimestamp());
    }

    public static <T> Result<T> fail(ErrorCodeType errorCode, String message) {
        return fail(errorCode.getCode(), message);
    }

    public static <T> Result<T> fail(Throwable throwable) {
        if (throwable instanceof BizException bizException) {
            return fail(bizException.getErrorCode(), bizException.getMessage());
        }
        if (throwable instanceof BaseException baseException) {
            return fail(baseException.getErrorCode());
        }
        return fail(ErrorCode.SYSTEM_ERROR);
    }

    private static long currentTimestamp() {
        return System.currentTimeMillis();
    }
}
