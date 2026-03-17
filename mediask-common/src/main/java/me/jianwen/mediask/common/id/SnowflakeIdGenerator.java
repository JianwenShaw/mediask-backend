package me.jianwen.mediask.common.id;

import java.time.Instant;

public final class SnowflakeIdGenerator {

    private static final long CUSTOM_EPOCH = Instant.parse("2026-01-01T00:00:00Z").toEpochMilli();
    private static final long SEQUENCE_BITS = 12L;
    private static final long WORKER_ID_BITS = 10L;
    private static final long MAX_SEQUENCE = ~(-1L << SEQUENCE_BITS);
    private static final long WORKER_ID_SHIFT = SEQUENCE_BITS;
    private static final long TIMESTAMP_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS;
    private static final long DEFAULT_WORKER_ID = 1L;

    private static long sequence = 0L;
    private static long lastTimestamp = -1L;

    private SnowflakeIdGenerator() {
    }

    public static synchronized long nextId() {
        long currentTimestamp = currentTimestamp();
        if (currentTimestamp < lastTimestamp) {
            throw new IllegalStateException("Clock moved backwards for snowflake generator");
        }

        if (currentTimestamp == lastTimestamp) {
            sequence = (sequence + 1) & MAX_SEQUENCE;
            if (sequence == 0L) {
                currentTimestamp = waitUntilNextMillis(currentTimestamp);
            }
        } else {
            sequence = 0L;
        }

        lastTimestamp = currentTimestamp;
        return ((currentTimestamp - CUSTOM_EPOCH) << TIMESTAMP_SHIFT)
                | (DEFAULT_WORKER_ID << WORKER_ID_SHIFT)
                | sequence;
    }

    private static long waitUntilNextMillis(long currentTimestamp) {
        long nextTimestamp = currentTimestamp();
        while (nextTimestamp <= currentTimestamp) {
            nextTimestamp = currentTimestamp();
        }
        return nextTimestamp;
    }

    private static long currentTimestamp() {
        return System.currentTimeMillis();
    }
}
