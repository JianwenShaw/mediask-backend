package me.jianwen.mediask.domain.triage.model;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.regex.Pattern;

public record CatalogVersion(String value) {

    private static final Pattern FORMAT = Pattern.compile("deptcat-v\\d{8}-\\d{2}");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.BASIC_ISO_DATE;

    public CatalogVersion {
        if (value == null || !FORMAT.matcher(value).matches()) {
            throw new IllegalArgumentException("invalid catalog version format: " + value);
        }
    }

    public static CatalogVersion of(LocalDate date, int sequence) {
        if (sequence < 1 || sequence > 99) {
            throw new IllegalArgumentException("sequence must be 1-99, got: " + sequence);
        }
        return new CatalogVersion("deptcat-v" + date.format(DATE_FMT) + "-" + String.format("%02d", sequence));
    }

    public LocalDate date() {
        // "deptcat-v" is 9 chars, date is 8 chars → substring(9, 17)
        return LocalDate.parse(value.substring(9, 17), DATE_FMT);
    }

    public int sequence() {
        // "deptcat-v20260428-" is 18 chars, sequence is 2 chars → substring(18)
        return Integer.parseInt(value.substring(18));
    }

    public boolean isSameDay(LocalDate date) {
        return date().equals(date);
    }
}
