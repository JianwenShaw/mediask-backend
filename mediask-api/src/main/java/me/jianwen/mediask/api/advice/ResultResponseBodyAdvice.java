package me.jianwen.mediask.api.advice;

import me.jianwen.mediask.api.context.ApiRequestContext;
import me.jianwen.mediask.common.request.RequestConstants;
import me.jianwen.mediask.common.result.Result;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

@RestControllerAdvice
public class ResultResponseBodyAdvice implements ResponseBodyAdvice<Object> {

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return true;
    }

    @Override
    public Object beforeBodyWrite(
            Object body,
            MethodParameter returnType,
            MediaType selectedContentType,
            Class<? extends HttpMessageConverter<?>> selectedConverterType,
            ServerHttpRequest request,
            ServerHttpResponse response) {
        if (!(body instanceof Result<?> result)) {
            return body;
        }

        String requestId = ApiRequestContext.currentRequestIdOrGenerate();
        response.getHeaders().set(RequestConstants.REQUEST_ID_HEADER, requestId);
        if (result.requestId() != null && !result.requestId().isBlank()) {
            return result;
        }
        return result.withRequestId(requestId);
    }
}
