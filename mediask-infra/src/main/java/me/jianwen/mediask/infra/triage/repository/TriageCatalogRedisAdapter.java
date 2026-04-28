package me.jianwen.mediask.infra.triage.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import me.jianwen.mediask.common.cache.CacheKeyGenerator;
import me.jianwen.mediask.common.exception.SysException;
import me.jianwen.mediask.domain.triage.model.CatalogVersion;
import me.jianwen.mediask.domain.triage.model.DepartmentCandidate;
import me.jianwen.mediask.domain.triage.model.TriageCatalog;
import me.jianwen.mediask.domain.triage.port.TriageCatalogPublishPort;
import me.jianwen.mediask.infra.triage.cache.TriageCatalogCachePolicy;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class TriageCatalogRedisAdapter implements TriageCatalogPublishPort {

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    private static final DateTimeFormatter WIRE_DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");

    public TriageCatalogRedisAdapter(StringRedisTemplate stringRedisTemplate, ObjectMapper objectMapper) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public void publish(TriageCatalog catalog) {
        String contentKey = CacheKeyGenerator.triageCatalogContent(
                catalog.hospitalScope(), catalog.catalogVersion().value());
        String activeKey = CacheKeyGenerator.triageCatalogActiveVersion(catalog.hospitalScope());

        try {
            String json = objectMapper.writeValueAsString(TriageCatalogJson.fromDomain(catalog));
            // Step 1: write content (no TTL — immutable, kept forever for historical validation)
            stringRedisTemplate.opsForValue().set(contentKey, json);
            // Step 2: switch active pointer
            stringRedisTemplate.opsForValue().set(activeKey, catalog.catalogVersion().value());
        } catch (JsonProcessingException e) {
            throw new SysException("Failed to serialize triage catalog", e);
        }
    }

    @Override
    public Optional<TriageCatalog> findActiveCatalog(String hospitalScope) {
        return findActiveVersion(hospitalScope)
                .flatMap(version -> findCatalogByVersion(hospitalScope, version));
    }

    @Override
    public Optional<TriageCatalog> findCatalogByVersion(String hospitalScope, CatalogVersion version) {
        String key = CacheKeyGenerator.triageCatalogContent(hospitalScope, version.value());
        String json = stringRedisTemplate.opsForValue().get(key);
        if (json == null || json.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(objectMapper.readValue(json, TriageCatalogJson.class).toDomain());
        } catch (JsonProcessingException e) {
            throw new SysException("Failed to deserialize triage catalog: " + key, e);
        }
    }

    @Override
    public Optional<CatalogVersion> findActiveVersion(String hospitalScope) {
        String key = CacheKeyGenerator.triageCatalogActiveVersion(hospitalScope);
        String versionStr = stringRedisTemplate.opsForValue().get(key);
        if (versionStr == null || versionStr.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(new CatalogVersion(versionStr));
        } catch (IllegalArgumentException e) {
            throw new SysException("Invalid active triage catalog version: " + key, e);
        }
    }

    @Override
    public CatalogVersion nextVersion(String hospitalScope) {
        LocalDate today = LocalDate.now();
        String counterKey = CacheKeyGenerator.triageCatalogSequenceCounter(
                hospitalScope, today.format(DateTimeFormatter.BASIC_ISO_DATE));
        Long sequence = stringRedisTemplate.opsForValue().increment(counterKey);
        stringRedisTemplate.expire(counterKey, TriageCatalogCachePolicy.SEQUENCE_COUNTER_TTL);
        if (sequence == null) {
            throw new SysException(
                    "Failed to allocate triage catalog version sequence",
                    new IllegalStateException("Redis INCR returned null"));
        }
        return CatalogVersion.of(today, Math.toIntExact(sequence));
    }

    private record TriageCatalogJson(
            @JsonProperty("hospital_scope") String hospitalScope,
            @JsonProperty("catalog_version") String catalogVersion,
            @JsonProperty("published_at") String publishedAt,
            @JsonProperty("department_candidates") List<DepartmentCandidateJson> departmentCandidates) {

        private static TriageCatalogJson fromDomain(TriageCatalog catalog) {
            return new TriageCatalogJson(
                    catalog.hospitalScope(),
                    catalog.catalogVersion().value(),
                    catalog.publishedAt().format(WIRE_DATE_TIME_FORMATTER),
                    catalog.departmentCandidates().stream()
                            .map(DepartmentCandidateJson::fromDomain)
                            .toList());
        }

        private TriageCatalog toDomain() {
            return new TriageCatalog(
                    hospitalScope,
                    new CatalogVersion(catalogVersion),
                    OffsetDateTime.parse(publishedAt),
                    departmentCandidates.stream()
                            .map(DepartmentCandidateJson::toDomain)
                            .toList());
        }
    }

    private record DepartmentCandidateJson(
            @JsonProperty("department_id") Long departmentId,
            @JsonProperty("department_name") String departmentName,
            @JsonProperty("routing_hint") String routingHint,
            @JsonProperty("aliases") List<String> aliases,
            @JsonProperty("sort_order") int sortOrder) {

        private static DepartmentCandidateJson fromDomain(DepartmentCandidate candidate) {
            return new DepartmentCandidateJson(
                    candidate.departmentId(),
                    candidate.departmentName(),
                    candidate.routingHint(),
                    candidate.aliases(),
                    candidate.sortOrder());
        }

        private DepartmentCandidate toDomain() {
            return new DepartmentCandidate(
                    departmentId,
                    departmentName,
                    routingHint,
                    aliases,
                    sortOrder);
        }
    }
}
