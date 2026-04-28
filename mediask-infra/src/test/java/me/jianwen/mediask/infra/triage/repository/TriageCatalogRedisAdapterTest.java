package me.jianwen.mediask.infra.triage.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.lang.reflect.Proxy;
import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import me.jianwen.mediask.common.cache.CacheKeyGenerator;
import me.jianwen.mediask.common.exception.SysException;
import me.jianwen.mediask.domain.triage.model.CatalogVersion;
import me.jianwen.mediask.domain.triage.model.DepartmentCandidate;
import me.jianwen.mediask.domain.triage.model.TriageCatalog;
import me.jianwen.mediask.infra.triage.cache.TriageCatalogCachePolicy;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

class TriageCatalogRedisAdapterTest {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Test
    void publish_WhenCatalogProvided_WritesContractJsonAndActivePointer() throws Exception {
        FakeStringRedisTemplate redisTemplate = new FakeStringRedisTemplate();
        TriageCatalogRedisAdapter adapter = new TriageCatalogRedisAdapter(redisTemplate, objectMapper);
        TriageCatalog catalog = new TriageCatalog(
                "default",
                new CatalogVersion("deptcat-v20260423-01"),
                OffsetDateTime.parse("2026-04-23T12:00:00Z"),
                List.of(new DepartmentCandidate(101L, "神经内科", "头痛头晕", List.of("神内"), 10)));

        adapter.publish(catalog);

        String contentKey = CacheKeyGenerator.triageCatalogContent("default", "deptcat-v20260423-01");
        String activeKey = CacheKeyGenerator.triageCatalogActiveVersion("default");
        assertEquals("deptcat-v20260423-01", redisTemplate.store.get(activeKey));
        JsonNode json = objectMapper.readTree(redisTemplate.store.get(contentKey));
        assertEquals("default", json.get("hospital_scope").asText());
        assertEquals("deptcat-v20260423-01", json.get("catalog_version").asText());
        assertEquals("2026-04-23T12:00:00Z", json.get("published_at").asText());
        assertEquals(101L, json.get("department_candidates").get(0).get("department_id").asLong());
        assertEquals("神经内科", json.get("department_candidates").get(0).get("department_name").asText());
        assertEquals("头痛头晕", json.get("department_candidates").get(0).get("routing_hint").asText());
        assertEquals("神内", json.get("department_candidates").get(0).get("aliases").get(0).asText());
        assertEquals(10, json.get("department_candidates").get(0).get("sort_order").asInt());
        assertFalse(json.has("hospitalScope"));
        assertFalse(json.has("departmentCandidates"));
    }

    @Test
    void findCatalogByVersion_WhenContractJsonPresent_ReturnsDomainCatalog() {
        FakeStringRedisTemplate redisTemplate = new FakeStringRedisTemplate();
        TriageCatalogRedisAdapter adapter = new TriageCatalogRedisAdapter(redisTemplate, objectMapper);
        redisTemplate.store.put(
                CacheKeyGenerator.triageCatalogContent("default", "deptcat-v20260423-01"),
                """
                {"hospital_scope":"default","catalog_version":"deptcat-v20260423-01","published_at":"2026-04-23T12:00:00Z","department_candidates":[{"department_id":101,"department_name":"神经内科","routing_hint":"头痛头晕","aliases":["神内"],"sort_order":10}]}
                """.trim());

        TriageCatalog catalog = adapter.findCatalogByVersion("default", new CatalogVersion("deptcat-v20260423-01"))
                .orElseThrow();

        assertEquals("default", catalog.hospitalScope());
        assertEquals("deptcat-v20260423-01", catalog.catalogVersion().value());
        assertEquals(1, catalog.candidateCount());
        assertEquals("神经内科", catalog.departmentCandidates().get(0).departmentName());
    }

    @Test
    void findActiveVersion_WhenStoredVersionInvalid_ThrowsSysException() {
        FakeStringRedisTemplate redisTemplate = new FakeStringRedisTemplate();
        TriageCatalogRedisAdapter adapter = new TriageCatalogRedisAdapter(redisTemplate, objectMapper);
        redisTemplate.store.put(CacheKeyGenerator.triageCatalogActiveVersion("default"), "bad-version");

        assertThrows(SysException.class, () -> adapter.findActiveVersion("default"));
    }

    @Test
    void nextVersion_WhenCalledTwice_UsesAtomicRedisIncrementSequence() {
        FakeStringRedisTemplate redisTemplate = new FakeStringRedisTemplate();
        TriageCatalogRedisAdapter adapter = new TriageCatalogRedisAdapter(redisTemplate, objectMapper);
        String today = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);

        CatalogVersion first = adapter.nextVersion("default");
        CatalogVersion second = adapter.nextVersion("default");

        assertEquals("deptcat-v" + today + "-01", first.value());
        assertEquals("deptcat-v" + today + "-02", second.value());
        assertEquals(
                TriageCatalogCachePolicy.SEQUENCE_COUNTER_TTL,
                redisTemplate.ttls.get(CacheKeyGenerator.triageCatalogSequenceCounter("default", today)));
    }

    private static final class FakeStringRedisTemplate extends StringRedisTemplate {

        private final Map<String, String> store = new HashMap<>();
        private final Map<String, Duration> ttls = new HashMap<>();

        @Override
        public ValueOperations<String, String> opsForValue() {
            Object proxy = Proxy.newProxyInstance(
                    ValueOperations.class.getClassLoader(),
                    new Class<?>[] {ValueOperations.class},
                    (instance, method, args) -> switch (method.getName()) {
                        case "get" -> store.get(args[0]);
                        case "set" -> {
                            store.put((String) args[0], (String) args[1]);
                            yield null;
                        }
                        case "increment" -> {
                            String key = (String) args[0];
                            long next = Long.parseLong(store.getOrDefault(key, "0")) + 1;
                            store.put(key, Long.toString(next));
                            yield next;
                        }
                        case "equals" -> instance == args[0];
                        case "hashCode" -> System.identityHashCode(instance);
                        case "toString" -> "FakeValueOperations";
                        default -> throw new UnsupportedOperationException(method.getName());
                    });
            return ValueOperations.class.cast(proxy);
        }

        @Override
        public Boolean expire(String key, Duration timeout) {
            ttls.put(key, timeout);
            return true;
        }
    }
}
