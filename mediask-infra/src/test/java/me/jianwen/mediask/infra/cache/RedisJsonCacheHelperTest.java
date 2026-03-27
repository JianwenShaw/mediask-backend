package me.jianwen.mediask.infra.cache;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.lang.reflect.Proxy;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import me.jianwen.mediask.common.exception.SysException;
import me.jianwen.mediask.domain.user.model.DoctorProfile;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

class RedisJsonCacheHelperTest {

    @Test
    void get_WhenValuePresent_ReturnDeserializedObject() {
        FakeStringRedisTemplate stringRedisTemplate = new FakeStringRedisTemplate();
        RedisJsonCacheHelper helper = new RedisJsonCacheHelper(stringRedisTemplate, new ObjectMapper());
        String key = "user:doctor-profile:1001";
        stringRedisTemplate.store.put(
                key,
                """
                {"doctorId":1001,"doctorCode":"DOC-1001","professionalTitle":"Chief Physician","introductionMasked":"Experienced","hospitalId":12,"primaryDepartmentId":7,"primaryDepartmentName":"Cardiology"}
                """.trim());

        Optional<DoctorProfile> cachedDoctorProfile = helper.get(key, DoctorProfile.class);

        assertTrue(cachedDoctorProfile.isPresent());
        assertEquals("DOC-1001", cachedDoctorProfile.orElseThrow().doctorCode());
    }

    @Test
    void get_WhenJsonInvalid_ThrowSysException() {
        FakeStringRedisTemplate stringRedisTemplate = new FakeStringRedisTemplate();
        RedisJsonCacheHelper helper = new RedisJsonCacheHelper(stringRedisTemplate, new ObjectMapper());
        String key = "user:doctor-profile:1001";
        stringRedisTemplate.store.put(key, "{invalid-json");

        assertThrows(SysException.class, () -> helper.get(key, DoctorProfile.class));
    }

    @Test
    void put_WhenValueProvided_StoreSerializedJson() {
        FakeStringRedisTemplate stringRedisTemplate = new FakeStringRedisTemplate();
        RedisJsonCacheHelper helper = new RedisJsonCacheHelper(stringRedisTemplate, new ObjectMapper());
        String key = "user:doctor-profile:1001";

        helper.put(
                key,
                new DoctorProfile(1001L, "DOC-1001", "Chief Physician", "Experienced", 12L, 7L, "Cardiology"),
                Duration.ofMinutes(10));

        assertTrue(stringRedisTemplate.store.get(key).contains("\"doctorCode\":\"DOC-1001\""));
        assertEquals(Duration.ofMinutes(10), stringRedisTemplate.lastTtl);
    }

    private static final class FakeStringRedisTemplate extends StringRedisTemplate {

        private final Map<String, String> store = new HashMap<>();
        private Duration lastTtl;

        @Override
        public ValueOperations<String, String> opsForValue() {
            Object proxy = Proxy.newProxyInstance(
                    ValueOperations.class.getClassLoader(),
                    new Class<?>[] {ValueOperations.class},
                    (instance, method, args) -> {
                        return switch (method.getName()) {
                            case "get" -> store.get(args[0]);
                            case "set" -> {
                                if (args.length == 2) {
                                    store.put((String) args[0], (String) args[1]);
                                } else if (args.length == 3 && args[2] instanceof Duration duration) {
                                    store.put((String) args[0], (String) args[1]);
                                    lastTtl = duration;
                                } else {
                                    throw new UnsupportedOperationException(method.getName());
                                }
                                yield null;
                            }
                            case "equals" -> instance == args[0];
                            case "hashCode" -> System.identityHashCode(instance);
                            case "toString" -> "FakeValueOperations";
                            default -> throw new UnsupportedOperationException(method.getName());
                        };
                    });
            return ValueOperations.class.cast(proxy);
        }
    }
}
