package me.jianwen.mediask.common.exception;

public class BizException extends BaseException {

    public BizException(ErrorCodeType errorCode) {
        super(errorCode);
    }

    public BizException(ErrorCodeType errorCode, String message) {
        super(errorCode, message);
    }

    public BizException(ErrorCodeType errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }
}
