package me.jianwen.mediask.api.assembler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import me.jianwen.mediask.api.context.ApiRequestContext;
import me.jianwen.mediask.api.dto.CreateKnowledgeBaseRequest;
import me.jianwen.mediask.api.dto.PublishKnowledgeReleaseRequest;
import me.jianwen.mediask.api.dto.UpdateKnowledgeBaseRequest;
import me.jianwen.mediask.api.security.AuthenticatedUserPrincipal;
import me.jianwen.mediask.common.exception.ErrorCode;
import me.jianwen.mediask.common.exception.SysException;
import me.jianwen.mediask.domain.ai.model.CreateKnowledgeBasePayload;
import me.jianwen.mediask.domain.ai.model.KnowledgeAdminFile;
import me.jianwen.mediask.domain.ai.model.KnowledgeAdminGatewayContext;
import me.jianwen.mediask.domain.ai.model.KnowledgeBaseListQuery;
import me.jianwen.mediask.domain.ai.model.KnowledgeDocumentListQuery;
import me.jianwen.mediask.domain.ai.model.PublishKnowledgeReleasePayload;
import me.jianwen.mediask.domain.ai.model.UpdateKnowledgeBasePayload;
import org.springframework.web.multipart.MultipartFile;

public final class KnowledgeAdminAssembler {

    private static final String DEFAULT_HOSPITAL_SCOPE = "default";

    private KnowledgeAdminAssembler() {
    }

    public static KnowledgeAdminGatewayContext toContext(AuthenticatedUserPrincipal principal) {
        return new KnowledgeAdminGatewayContext(
                ApiRequestContext.currentRequestIdOrGenerate(),
                principal.userId(),
                DEFAULT_HOSPITAL_SCOPE);
    }

    public static KnowledgeBaseListQuery toKnowledgeBaseListQuery(
            String keyword,
            Integer pageNum,
            Integer pageSize) {
        return new KnowledgeBaseListQuery(keyword, pageNum, pageSize);
    }

    public static CreateKnowledgeBasePayload toCreateKnowledgeBasePayload(CreateKnowledgeBaseRequest request) {
        return new CreateKnowledgeBasePayload(
                request.code(),
                request.name(),
                request.description(),
                request.defaultEmbeddingModel(),
                request.defaultEmbeddingDimension(),
                request.retrievalStrategy());
    }

    public static UpdateKnowledgeBasePayload toUpdateKnowledgeBasePayload(UpdateKnowledgeBaseRequest request) {
        return new UpdateKnowledgeBasePayload(
                request.name(),
                request.description(),
                request.status());
    }

    public static KnowledgeDocumentListQuery toKnowledgeDocumentListQuery(
            String knowledgeBaseId,
            Integer pageNum,
            Integer pageSize) {
        return new KnowledgeDocumentListQuery(knowledgeBaseId, pageNum, pageSize);
    }

    public static PublishKnowledgeReleasePayload toPublishKnowledgeReleasePayload(
            PublishKnowledgeReleaseRequest request) {
        return new PublishKnowledgeReleasePayload(
                request.knowledgeBaseId(),
                request.targetIndexVersionId());
    }

    public static KnowledgeAdminFile toKnowledgeAdminFile(MultipartFile file) {
        try {
            return new KnowledgeAdminFile(file.getOriginalFilename(), file.getContentType(), file.getBytes());
        } catch (IOException exception) {
            throw new SysException(ErrorCode.SYSTEM_ERROR, "failed to read uploaded knowledge document", exception);
        }
    }

    public static Object toFrontendPayload(Object value) {
        return convertKeysToCamelCase(value);
    }

    private static Object convertKeysToCamelCase(Object value) {
        if (value instanceof Map<?, ?> map) {
            LinkedHashMap<String, Object> converted = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                String key = String.valueOf(entry.getKey());
                converted.put(toCamelCase(key), convertKeysToCamelCase(entry.getValue()));
            }
            return converted;
        }
        if (value instanceof List<?> list) {
            List<Object> converted = new ArrayList<>(list.size());
            for (Object item : list) {
                converted.add(convertKeysToCamelCase(item));
            }
            return converted;
        }
        return value;
    }

    private static String toCamelCase(String value) {
        StringBuilder converted = new StringBuilder();
        boolean capitalizeNext = false;
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            if (character == '_') {
                capitalizeNext = true;
            } else if (capitalizeNext) {
                converted.append(Character.toUpperCase(character));
                capitalizeNext = false;
            } else {
                converted.append(character);
            }
        }
        return converted.toString();
    }
}
