package com.madre.excelweb.service;

import org.springframework.beans.factory.annotation.Value;

public class IdGenerationService {

    private static final long NODE_ID_BITS = 10L;
    private static final long SEQUENCE_BITS = 12L;
    private static final long MAX_NODE_ID = (1L << NODE_ID_BITS) - 1;
    private static final long MAX_SEQUENCE = (1L << SEQUENCE_BITS) - 1;
    private static final long TIMESTAMP_SHIFT = NODE_ID_BITS + SEQUENCE_BITS;
    private static final long NODE_ID_SHIFT = SEQUENCE_BITS;
    private final long epoch = 1626240000000L;

    @Value("${web.app.id.node-id}")
    private long nodeId;
    private long lastTimestamp = -1L;
    private long sequence = 0L;


    public synchronized long nextId() {
        if (nodeId < 0 || nodeId > MAX_NODE_ID) {
            throw new IllegalStateException(String.format("Node Id must be between %s and %s", 0, MAX_NODE_ID));
        }
        long currentTimestamp = System.currentTimeMillis();
        if (currentTimestamp < lastTimestamp) {
            throw new IllegalStateException("Invalid System Clock!");
        }

        if (currentTimestamp == lastTimestamp) {
            sequence = (sequence + 1) & MAX_SEQUENCE;
            if (sequence == 0) {
                currentTimestamp = waitUntilNextMillis(currentTimestamp);
            }
        } else {
            sequence = 0;
        }

        lastTimestamp = currentTimestamp;

        return ((currentTimestamp - epoch) << TIMESTAMP_SHIFT) | (nodeId << NODE_ID_SHIFT) | sequence;

    }

    private long waitUntilNextMillis(long lastTimestamp) {
        long timestamp = System.currentTimeMillis();
        while (timestamp <= lastTimestamp) {
            timestamp = System.currentTimeMillis();
        }
        return timestamp;
    }
}
