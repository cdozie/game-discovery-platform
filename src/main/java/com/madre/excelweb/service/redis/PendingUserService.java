package com.madre.excelweb.service.redis;

import com.madre.excelweb.jpa.entity.AppUser;
import com.madre.excelweb.jpa.entity.EmailVerificationToken;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Slf4j
@RequiredArgsConstructor
public class PendingUserService {
    private static final String PENDING_USER_KEY_TEMPLATE = "pending-user:%s";
    private final RedisTemplate<String, AppUser> redisTemplate;

    public void savePendingUser(AppUser appUser) {
        try {
            log.info("Saving pending user: {}", appUser);
            redisTemplate.opsForValue().set(PENDING_USER_KEY_TEMPLATE.formatted(appUser.getEmail()), appUser, EmailVerificationToken.EXPIRATION, TimeUnit.MINUTES);
            log.info("Pending user successfully saved: {}", appUser);

        } catch (Exception e) {
            log.error("Error saving pending user: {}", appUser.getUsername(), e);
            throw e;
        }

    }

    public Optional<AppUser> getPendingUser(String email) {
        return Optional.ofNullable(redisTemplate.opsForValue().get(PENDING_USER_KEY_TEMPLATE.formatted(email)));
    }

    public void removePendingUser(String email) {
        try {
            log.info("Removing pending user: {}", email);
            redisTemplate.delete(PENDING_USER_KEY_TEMPLATE.formatted(email));
            log.info("Pending user successfully removed: {}", email);
        } catch (Exception e) {
            log.error("Error removing pending user with email: {}", email, e);
            throw e;
        }
    }

}
