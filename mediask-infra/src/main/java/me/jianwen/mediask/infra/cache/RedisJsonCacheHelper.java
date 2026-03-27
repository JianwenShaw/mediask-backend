package me.jianwen.mediask.infra.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.Optional;
import me.jianwen.mediask.common.exception.SysException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class RedisJsonCacheHelper {

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    public RedisJsonCacheHelper(StringRedisTemplate stringRedisTemplate, ObjectMapper objectMapper) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
    }

    public <T> Optional<T> get(String key, Class<T> valueType) {
        String cachedValue = stringRedisTemplate.opsForValue().get(key);
        if (cachedValue == null || cachedValue.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(objectMapper.readValue(cachedValue, valueType));
        } catch (JsonProcessingException exception) {
            throw new SysException("Failed to deserialize Redis cached value for key: " + key, exception);
        }
    }

    public void put(String key, Object value, Duration ttl) {
        try {
            String serializedValue = objectMapper.writeValueAsString(value);
            stringRedisTemplate.opsForValue().set(key, serializedValue, ttl);
        } catch (JsonProcessingException exception) {
            throw new SysException("Failed to serialize Redis cached value for key: " + key, exception);
        }
    }

    public void delete(String key) {
        stringRedisTemplate.delete(key);
    }
}
