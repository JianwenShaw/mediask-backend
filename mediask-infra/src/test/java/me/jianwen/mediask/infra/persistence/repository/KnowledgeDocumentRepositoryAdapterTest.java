package me.jianwen.mediask.infra.persistence.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Proxy;
import java.util.Map;
import me.jianwen.mediask.common.exception.BizException;
import me.jianwen.mediask.domain.ai.exception.AiErrorCode;
import me.jianwen.mediask.domain.ai.model.KnowledgeDocument;
import me.jianwen.mediask.domain.ai.model.KnowledgeSourceType;
import me.jianwen.mediask.infra.persistence.dataobject.KnowledgeDocumentDO;
import me.jianwen.mediask.infra.persistence.mapper.KnowledgeChunkMapper;
import me.jianwen.mediask.infra.persistence.mapper.KnowledgeDocumentMapper;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;

class KnowledgeDocumentRepositoryAdapterTest {

    @Test
    void save_WhenContentHashConflict_ThrowBizException() {
        KnowledgeDocumentRepositoryAdapter adapter = new KnowledgeDocumentRepositoryAdapter(
                proxy(Map.of(
                        "insertKnowledgeDocument", arguments -> {
                            throw new DuplicateKeyException(
                                    "duplicate key value violates unique constraint \"uk_knowledge_document_base_hash_active\"");
                        })),
                proxyChunkMapper(Map.of()));

        BizException exception = assertThrows(
                BizException.class,
                () -> adapter.save(KnowledgeDocument.createUploaded(
                        4001L, "高血压指南", KnowledgeSourceType.MARKDOWN, "file:///tmp/guide.md", "hash-1")));

        assertEquals(AiErrorCode.KNOWLEDGE_DOCUMENT_DUPLICATE.getCode(), exception.getCode());
    }

    @Test
    void save_ShouldConvertDocumentUuidToStringBeforeInsert() {
        final KnowledgeDocumentDO[] inserted = new KnowledgeDocumentDO[1];
        KnowledgeDocument document =
                KnowledgeDocument.createUploaded(4001L, "高血压指南", KnowledgeSourceType.MARKDOWN, "file:///tmp/guide.md", "hash-1");
        KnowledgeDocumentRepositoryAdapter adapter = new KnowledgeDocumentRepositoryAdapter(
                proxy(Map.of("insertKnowledgeDocument", arguments -> {
                    inserted[0] = (KnowledgeDocumentDO) arguments[0];
                    return 1;
                })),
                proxyChunkMapper(Map.of()));

        adapter.save(document);

        assertEquals(document.documentUuid().toString(), inserted[0].getDocumentUuid());
    }

    @Test
    void deleteById_WhenExisting_ShouldSoftDeleteDocumentAndChunks() {
        KnowledgeDocumentDO existing = new KnowledgeDocumentDO();
        existing.setId(5001L);
        existing.setVersion(3);
        final Object[][] updatedDocument = new Object[1][];
        final Object[][] updatedChunks = new Object[1][];
        KnowledgeDocumentRepositoryAdapter adapter = new KnowledgeDocumentRepositoryAdapter(
                proxy(Map.of(
                        "selectOne", arguments -> existing,
                        "update", arguments -> {
                            updatedDocument[0] = arguments;
                            return 1;
                        })),
                proxyChunkMapper(Map.of("update", arguments -> {
                    updatedChunks[0] = arguments;
                    return 2;
                })));

        adapter.deleteById(5001L);

        assertNull(updatedDocument[0][0]);
        assertTrue(updatedChunks[0] != null);
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

    private static KnowledgeChunkMapper proxyChunkMapper(Map<String, java.util.function.Function<Object[], Object>> handlers) {
        return (KnowledgeChunkMapper) Proxy.newProxyInstance(
                KnowledgeChunkMapper.class.getClassLoader(),
                new Class<?>[] {KnowledgeChunkMapper.class},
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
