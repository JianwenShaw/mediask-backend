package me.jianwen.mediask.api.logging;

import ch.qos.logback.classic.spi.ILoggingEvent;
import java.util.Map;
import me.jianwen.mediask.common.request.RequestConstants;
import org.springframework.boot.json.JsonWriter.Members;
import org.springframework.boot.logging.structured.StructuredLoggingJsonMembersCustomizer;

public class ApiStructuredLoggingJsonCustomizer
        implements StructuredLoggingJsonMembersCustomizer<ILoggingEvent> {

    @Override
    public void customize(Members<ILoggingEvent> members) {
        members.add("request_id", event -> mdc(event).get(RequestConstants.MDC_REQUEST_ID)).whenHasLength();
        members.add("request_uri", event -> mdc(event).get(RequestConstants.MDC_REQUEST_URI)).whenHasLength();
        members.add("user_id", event -> mdc(event).get(RequestConstants.MDC_USER_ID)).whenHasLength();
    }

    private Map<String, String> mdc(ILoggingEvent event) {
        return event.getMDCPropertyMap();
    }
}
