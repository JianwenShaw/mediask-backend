package me.jianwen.mediask.infra.ai.client.support;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

public final class AiServiceSseEventReader {

    public void read(InputStream body, Consumer<SseEventFrame> eventConsumer) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(body, StandardCharsets.UTF_8))) {
            String currentEventName = null;
            StringBuilder currentData = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isEmpty()) {
                    dispatch(currentEventName, currentData, eventConsumer);
                    currentEventName = null;
                    currentData.setLength(0);
                    continue;
                }
                if (line.startsWith("event:")) {
                    currentEventName = normalizeValue(line.substring("event:".length()));
                    continue;
                }
                if (line.startsWith("data:")) {
                    if (!currentData.isEmpty()) {
                        currentData.append('\n');
                    }
                    currentData.append(normalizeValue(line.substring("data:".length())));
                }
            }
            dispatch(currentEventName, currentData, eventConsumer);
        }
    }

    private void dispatch(String eventName, StringBuilder data, Consumer<SseEventFrame> eventConsumer) {
        if (eventName == null && data.isEmpty()) {
            return;
        }
        String normalizedEventName =
                eventName == null || eventName.isBlank() ? "message" : eventName;
        eventConsumer.accept(new SseEventFrame(normalizedEventName, data.toString()));
    }

    private String normalizeValue(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        return value.startsWith(" ") ? value.substring(1) : value;
    }
}
