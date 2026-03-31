package me.jianwen.mediask.application.authz;

import java.util.Map;
import java.util.Objects;
import java.util.Collections;

public record AuthzInvocationContext(ScenarioCode scenarioCode, AuthzSubject subject, Map<String, Object> arguments) {

    public AuthzInvocationContext {
        scenarioCode = Objects.requireNonNull(scenarioCode, "scenarioCode must not be null");
        subject = Objects.requireNonNull(subject, "subject must not be null");
        arguments = arguments == null ? Map.of() : Collections.unmodifiableMap(arguments);
    }
}
