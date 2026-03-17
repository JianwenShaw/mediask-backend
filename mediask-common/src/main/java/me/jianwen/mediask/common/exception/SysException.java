package me.jianwen.mediask.common.exception;

public class SysException extends BaseException {

    public SysException(ErrorCodeType errorCode) {
        super(errorCode);
    }

    public SysException(ErrorCodeType errorCode, String message) {
        super(errorCode, message);
    }

    public SysException(ErrorCodeType errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }

    public SysException(String message, Throwable cause) {
        super(ErrorCode.SYSTEM_ERROR, message, cause);
    }
}
