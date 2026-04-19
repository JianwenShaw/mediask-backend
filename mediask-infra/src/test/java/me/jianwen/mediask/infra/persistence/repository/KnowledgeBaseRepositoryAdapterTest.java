package me.jianwen.mediask.infra.persistence.repository;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import java.lang.reflect.Proxy;
import java.util.Map;
import me.jianwen.mediask.infra.persistence.dataobject.KnowledgeBaseDO;
import me.jianwen.mediask.infra.persistence.dataobject.KnowledgeChunkDO;
import me.jianwen.mediask.infra.persistence.dataobject.KnowledgeDocumentDO;
import me.jianwen.mediask.infra.persistence.mapper.KnowledgeBaseMapper;
import me.jianwen.mediask.infra.persistence.mapper.KnowledgeChunkMapper;
import me.jianwen.mediask.infra.persistence.mapper.KnowledgeDocumentMapper;
import org.junit.jupiter.api.Test;
import org.apache.ibatis.builder.MapperBuilderAssistant;

class KnowledgeBaseRepositoryAdapterTest {

    @Test
    void deleteById_WhenExisting_ShouldSoftDeleteKnowledgeBaseDocumentsAndChunks() {
        initTableInfo(KnowledgeBaseDO.class, KnowledgeDocumentDO.class, KnowledgeChunkDO.class);
        KnowledgeBaseDO existing = new KnowledgeBaseDO();
        existing.setId(4001L);
        existing.setVersion(2);
        final Object[][] updatedBase = new Object[1][];
        final Object[][] updatedDocuments = new Object[1][];
        final Object[][] updatedChunks = new Object[1][];
        KnowledgeBaseRepositoryAdapter adapter = new KnowledgeBaseRepositoryAdapter(
                proxyBaseMapper(Map.of(
                        "selectOne", arguments -> existing,
                        "update", arguments -> {
                            updatedBase[0] = arguments;
                            return 1;
                        })),
                proxyDocumentMapper(Map.of("update", arguments -> {
                    updatedDocuments[0] = arguments;
                    return 1;
                })),
                proxyChunkMapper(Map.of("update", arguments -> {
                    updatedChunks[0] = arguments;
                    return 1;
                })));

        adapter.deleteById(4001L);

        assertNull(updatedBase[0][0]);
        assertNull(updatedDocuments[0][0]);
        assertNull(updatedChunks[0][0]);
        assertTrue(updatedBase[0][1] != null);
        assertTrue(updatedDocuments[0][1] != null);
        assertTrue(updatedChunks[0][1] != null);
    }

    private static void initTableInfo(Class<?>... entityClasses) {
        MybatisConfiguration configuration = new MybatisConfiguration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "");
        for (Class<?> entityClass : entityClasses) {
            if (TableInfoHelper.getTableInfo(entityClass) == null) {
                TableInfoHelper.initTableInfo(assistant, entityClass);
            }
        }
    }

    private static KnowledgeBaseMapper proxyBaseMapper(Map<String, java.util.function.Function<Object[], Object>> handlers) {
        return (KnowledgeBaseMapper) Proxy.newProxyInstance(
                KnowledgeBaseMapper.class.getClassLoader(),
                new Class<?>[] {KnowledgeBaseMapper.class},
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

    private static KnowledgeDocumentMapper proxyDocumentMapper(
            Map<String, java.util.function.Function<Object[], Object>> handlers) {
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
