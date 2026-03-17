package me.jianwen.mediask.common.exception;

public interface ErrorCodeType {

    int getCode();

    String getMessage();

    ErrorCodeCategory getCategory();

    default boolean isSuccess() {
        return getCode() == 0;
    }
}
