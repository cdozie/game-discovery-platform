package com.madre.excelweb.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.time.Duration;
import java.util.Collections;
import java.util.concurrent.TimeUnit;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
public class RedissonLockClient {

    private static final int DEFAULT_LOCK_TIMEOUT = 10_000;
    private static final String LOCK_TIMEOUT = "redis.lock.timeout";
    private static final int DEFAULT_LEASE_TIME = 300_000;
    private static final String LEASE_TIME = "redis.lock.lease.time";
    private static final String LOCK_KEY_TEMPLATE = "lock:%s";
    private static final DefaultRedisScript<Long> RELEASE_LOCK_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end",
            Long.class);

    private final StringRedisTemplate stringRedisTemplate;
    private final Environment env;

    public boolean run(String lockPathName, Runnable runnable) {
        final String lockKey = LOCK_KEY_TEMPLATE.formatted(lockPathName);
        final String lockToken = UUID.randomUUID().toString();
        final long deadline = System.nanoTime()
                + TimeUnit.MILLISECONDS.toNanos(env.getProperty(LOCK_TIMEOUT, Integer.class, DEFAULT_LOCK_TIMEOUT));
        final Duration leaseTime = Duration.ofMillis(env.getProperty(LEASE_TIME, Integer.class, DEFAULT_LEASE_TIME));

        log.info("Attempting to acquire lock on {}", lockPathName);

        try {
            while (System.nanoTime() < deadline) {
                final Boolean acquired = stringRedisTemplate.opsForValue().setIfAbsent(lockKey, lockToken, leaseTime);
                if (Boolean.TRUE.equals(acquired)) {
                    log.info("Successfully acquired lock on {}.", lockPathName);
                    try {
                        runnable.run();
                    } catch (Exception e) {
                        log.error("Error running operation with lock on {}.", lockPathName, e);
                        throw e;
                    } finally {
                        stringRedisTemplate.execute(RELEASE_LOCK_SCRIPT, Collections.singletonList(lockKey), lockToken);
                        log.info("Successfully released lock on {}.", lockPathName);
                    }
                    return true;
                }
                TimeUnit.MILLISECONDS.sleep(100);
            }
            log.info("Failed to acquire lock on {} as is not free. Operation not being run.", lockPathName);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Error acquiring lock on {} as thread was interrupted.", lockPathName, e);
        }
        return false;
    }
}
