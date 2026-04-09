package me.jianwen.mediask.infra.ai.client.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class AiServiceSseEventReaderTest {

    private final AiServiceSseEventReader reader = new AiServiceSseEventReader();

    @Test
    void read_WhenEventsContainExplicitNames_ParseInOrder() throws Exception {
        String body = """
                event:message
                data:第一段

                event:meta
                data:{"risk_level":"low"}

                event:end
                data:{}

                """;

        List<SseEventFrame> frames = read(body);

        assertEquals(3, frames.size());
        assertEquals("message", frames.get(0).eventName());
        assertEquals("第一段", frames.get(0).data());
        assertEquals("meta", frames.get(1).eventName());
        assertEquals("{\"risk_level\":\"low\"}", frames.get(1).data());
        assertEquals("end", frames.get(2).eventName());
        assertEquals("{}", frames.get(2).data());
    }

    @Test
    void read_WhenEventNameMissing_DefaultToMessageAndKeepMultilineData() throws Exception {
        String body = """
                data:第一行
                data:第二行

                """;

        List<SseEventFrame> frames = read(body);

        assertEquals(1, frames.size());
        assertEquals("message", frames.get(0).eventName());
        assertEquals("第一行\n第二行", frames.get(0).data());
    }

    @Test
    void read_WhenBodyContainsEmptySeparators_IgnoreBlankFrames() throws Exception {
        String body = """

                event:error
                data:{"code":6001,"msg":"ai service unavailable"}


                """;

        List<SseEventFrame> frames = read(body);

        assertIterableEquals(
                List.of(new SseEventFrame("error", "{\"code\":6001,\"msg\":\"ai service unavailable\"}")),
                frames);
    }

    private List<SseEventFrame> read(String body) throws Exception {
        List<SseEventFrame> frames = new ArrayList<>();
        reader.read(new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8)), frames::add);
        return frames;
    }
}
