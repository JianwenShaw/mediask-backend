package me.jianwen.mediask.infra.ai.client.support;

public record AiServiceErrorResponse(Integer code, String msg, String requestId, Long timestamp) {
}
