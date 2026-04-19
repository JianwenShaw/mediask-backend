package me.jianwen.mediask.api.assembler;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

final class ApiDateTimeFormatter {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");

    private ApiDateTimeFormatter() {
    }

    static String format(OffsetDateTime value) {
        return value == null ? null : FORMATTER.format(value);
    }
}
