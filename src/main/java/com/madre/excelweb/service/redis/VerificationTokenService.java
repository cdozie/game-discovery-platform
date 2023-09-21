package com.madre.excelweb.service.redis;

import com.madre.excelweb.jpa.entity.EmailVerificationToken;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Slf4j
@RequiredArgsConstructor
public class VerificationTokenService {
    private static final String VERIFICATION_TOKEN_KEY_TEMPLATE = "verification-token:%s";
    private final RedisTemplate<String, EmailVerificationToken> redisTemplate;

    public void saveVerificationToken(EmailVerificationToken token) {
        try {
            log.info("Saving verification token: {}", token);
            redisTemplate.opsForValue().set(VERIFICATION_TOKEN_KEY_TEMPLATE.formatted(token.getEmail()), token, EmailVerificationToken.EXPIRATION, TimeUnit.MINUTES);
            log.info("Verification token successfully saved: {}", token);
        } catch (Exception e) {
            log.error("Error saving verification token: {}", token, e);
            throw e;
        }
    }

    public Optional<EmailVerificationToken> getVerificationToken(String email) {
        return Optional.ofNullable(redisTemplate.opsForValue().get(VERIFICATION_TOKEN_KEY_TEMPLATE.formatted(email)));
    }

    public void removeVerificationToken(String email) {
        try {
            log.info("Removing verification token for email: {}", email);
            final Boolean deleted = redisTemplate.delete(VERIFICATION_TOKEN_KEY_TEMPLATE.formatted(email));
            if (Boolean.FALSE.equals(deleted)) {
                log.warn("Verification token not found for email: {}", email);
            } else {
                log.info("Verification token successfully removed: {}", email);
            }
        } catch (Exception e) {
            log.error("Error removing verification token for email: {}", email, e);
            throw e;
        }
    }
}
