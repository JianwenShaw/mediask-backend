package me.jianwen.mediask.api.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

class JacksonConfigTest {

    @Test
    void longShouldSerializeAsString() throws Exception {
        ObjectMapper objectMapper = buildObjectMapper();

        String json = objectMapper.writeValueAsString(new IdPayload(36380131098365952L, 36380131098365952L));

        assertEquals("{\"id\":\"36380131098365952\",\"primitiveId\":\"36380131098365952\"}", json);
    }

    private ObjectMapper buildObjectMapper() {
        Jackson2ObjectMapperBuilder builder = new Jackson2ObjectMapperBuilder();
        new JacksonConfig().longToStringJacksonCustomizer().customize(builder);
        return builder.build().findAndRegisterModules();
    }

    private record IdPayload(Long id, long primitiveId) {}
}
