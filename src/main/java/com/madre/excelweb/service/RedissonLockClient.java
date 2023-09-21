package com.madre.excelweb.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.core.env.Environment;

import java.util.concurrent.TimeUnit;

@Slf4j
@RequiredArgsConstructor
public class RedissonLockClient {

    private static final int DEFAULT_LOCK_TIMEOUT = 10;
    private static final String LOCK_TIMEOUT = "redis.lock.timeout";
    private static final int DEFAULT_LEASE_TIME = -1;
    private static final String LEASE_TIME = "redis.lock.lease.time";


    private final RedissonClient redissonClient;
    private final Environment env;

    public void run(String lockPathName, Runnable runnable) {
        RLock lock = redissonClient.getLock(lockPathName);

        log.info("Attempting to acquire lock on {}", lockPathName);

        try {
            if (lock.tryLock(env.getProperty(LOCK_TIMEOUT, Integer.class, DEFAULT_LOCK_TIMEOUT),
                    env.getProperty(LEASE_TIME, Integer.class, DEFAULT_LEASE_TIME), TimeUnit.MILLISECONDS)) {
                log.info("Successfully acquired lock on {}.", lockPathName);
                try {
                    runnable.run();
                } catch (Exception e) {
                    log.error("Error running operation with lock on {}.", lockPathName, e);
                    throw e;
                } finally {
                    lock.unlock();
                    log.info("Successfully released lock on {}.", lockPathName);
                }
            } else {
                log.info("Failed to acquire lock on {} as is not free. Operation not being run.", lockPathName);
            }
        } catch (InterruptedException e) {
            log.error("Error acquiring lock on {} as thread was interrupted.", lockPathName, e);
        }
    }


}
