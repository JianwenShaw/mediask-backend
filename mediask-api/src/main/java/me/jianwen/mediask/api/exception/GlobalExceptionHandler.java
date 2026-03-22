package me.jianwen.mediask.api.exception;

import java.util.Objects;
import me.jianwen.mediask.api.context.ApiRequestContext;
import me.jianwen.mediask.common.exception.BizException;
import me.jianwen.mediask.common.exception.ErrorCode;
import me.jianwen.mediask.common.exception.ErrorCodeType;
import me.jianwen.mediask.common.exception.SysException;
import me.jianwen.mediask.common.request.RequestConstants;
import me.jianwen.mediask.common.result.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BizException.class)
    public ResponseEntity<Result<Void>> handleBizException(BizException exception) {
        String requestId = ApiRequestContext.currentRequestIdOrGenerate();
        logBusinessExceptionIfNeeded(exception, requestId);
        return buildResponse(
                resolveStatus(exception.getErrorCode()),
                Result.<Void>fail(exception.getErrorCode(), exception.getMessage())
                        .withRequestId(requestId),
                requestId);
    }

    @ExceptionHandler({
            MethodArgumentNotValidException.class,
            BindException.class,
            MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class,
            HttpMessageNotReadableException.class,
            IllegalArgumentException.class
    })
    public ResponseEntity<Result<Void>> handleBadRequest(Exception exception) {
        String requestId = ApiRequestContext.currentRequestIdOrGenerate();
        return buildResponse(
                HttpStatus.BAD_REQUEST,
                Result.<Void>fail(ErrorCode.INVALID_PARAMETER, resolveValidationMessage(exception))
                        .withRequestId(requestId),
                requestId);
    }

    @ExceptionHandler(SysException.class)
    public ResponseEntity<Result<Void>> handleSysException(SysException exception) {
        String requestId = ApiRequestContext.currentRequestIdOrGenerate();
        log.error(
                "System exception handled, requestId={}, code={}, message={}",
                requestId,
                exception.getCode(),
                exception.getMessage(),
                exception);
        return buildResponse(
                resolveStatus(exception.getErrorCode()),
                Result.<Void>fail(exception)
                        .withRequestId(requestId),
                requestId);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> handleException(Exception exception) {
        String requestId = ApiRequestContext.currentRequestIdOrGenerate();
        log.error("Unhandled exception, requestId={}", requestId, exception);
        return buildResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                Result.<Void>fail(ErrorCode.SYSTEM_ERROR)
                        .withRequestId(requestId),
                requestId);
    }

    private ResponseEntity<Result<Void>> buildResponse(HttpStatus status, Result<Void> body, String requestId) {
        return ResponseEntity.status(status)
                .header(RequestConstants.REQUEST_ID_HEADER, requestId)
                .body(body);
    }

    private void logBusinessExceptionIfNeeded(BizException exception, String requestId) {
        HttpStatus status = resolveStatus(exception.getErrorCode());
        if (status == HttpStatus.UNAUTHORIZED || status == HttpStatus.FORBIDDEN) {
            log.warn(
                    "Access denied, requestId={}, code={}, message={}",
                    requestId,
                    exception.getCode(),
                    exception.getMessage());
        }
    }

    private HttpStatus resolveStatus(ErrorCodeType errorCode) {
        return switch (errorCode.getCategory()) {
            case SUCCESS -> HttpStatus.OK;
            case BAD_REQUEST -> HttpStatus.BAD_REQUEST;
            case UNAUTHORIZED -> HttpStatus.UNAUTHORIZED;
            case FORBIDDEN -> HttpStatus.FORBIDDEN;
            case NOT_FOUND -> HttpStatus.NOT_FOUND;
            case CONFLICT -> HttpStatus.CONFLICT;
            case INTERNAL_ERROR -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }

    private String resolveValidationMessage(Exception exception) {
        if (exception instanceof MethodArgumentNotValidException methodArgumentNotValidException
                && methodArgumentNotValidException.getBindingResult().getFieldError() != null) {
            return Objects.requireNonNull(methodArgumentNotValidException.getBindingResult().getFieldError())
                    .getDefaultMessage();
        }
        if (exception instanceof BindException bindException && bindException.getFieldError() != null) {
            return Objects.requireNonNull(bindException.getFieldError()).getDefaultMessage();
        }
        return ErrorCode.INVALID_PARAMETER.getMessage();
    }
}
