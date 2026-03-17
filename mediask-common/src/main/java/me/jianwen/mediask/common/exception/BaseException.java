package me.jianwen.mediask.common.exception;

public abstract class BaseException extends RuntimeException {

    private final ErrorCodeType errorCode;
    private final int code;

    protected BaseException(ErrorCodeType errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.code = errorCode.getCode();
    }

    protected BaseException(ErrorCodeType errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.code = errorCode.getCode();
    }

    protected BaseException(ErrorCodeType errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.code = errorCode.getCode();
    }

    public ErrorCodeType getErrorCode() {
        return errorCode;
    }

    public int getCode() {
        return code;
    }
}
