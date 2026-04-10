package me.jianwen.mediask.infra.persistence.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.reflect.Proxy;
import java.util.Map;
import me.jianwen.mediask.common.exception.BizException;
import me.jianwen.mediask.domain.ai.exception.AiErrorCode;
import me.jianwen.mediask.domain.ai.model.KnowledgeDocument;
import me.jianwen.mediask.domain.ai.model.KnowledgeSourceType;
import me.jianwen.mediask.infra.persistence.mapper.KnowledgeDocumentMapper;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;

class KnowledgeDocumentRepositoryAdapterTest {

    @Test
    void save_WhenContentHashConflict_ThrowBizException() {
        KnowledgeDocumentRepositoryAdapter adapter = new KnowledgeDocumentRepositoryAdapter(proxy(Map.of(
                "insert", arguments -> {
                    throw new DuplicateKeyException(
                            "duplicate key value violates unique constraint \"uk_knowledge_document_base_hash\"");
                })));

        BizException exception = assertThrows(
                BizException.class,
                () -> adapter.save(KnowledgeDocument.createUploaded(
                        4001L, "高血压指南", KnowledgeSourceType.MARKDOWN, null, "hash-1")));

        assertEquals(AiErrorCode.KNOWLEDGE_DOCUMENT_DUPLICATE.getCode(), exception.getCode());
    }

    private static KnowledgeDocumentMapper proxy(Map<String, java.util.function.Function<Object[], Object>> handlers) {
        return (KnowledgeDocumentMapper) Proxy.newProxyInstance(
                KnowledgeDocumentMapper.class.getClassLoader(),
                new Class<?>[] {KnowledgeDocumentMapper.class},
                (proxy, method, args) -> {
                    java.util.function.Function<Object[], Object> handler = handlers.get(method.getName());
                    if (handler != null) {
                        return handler.apply(args);
                    }
                    Class<?> returnType = method.getReturnType();
                    if (returnType == boolean.class) {
                        return false;
                    }
                    if (returnType == int.class) {
                        return 0;
                    }
                    if (returnType == long.class) {
                        return 0L;
                    }
                    return null;
                });
    }
}
