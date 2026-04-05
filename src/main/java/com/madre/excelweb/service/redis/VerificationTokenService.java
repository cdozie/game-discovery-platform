package com.madre.excelweb.service.redis;

import com.madre.excelweb.jpa.entity.EmailVerificationToken;
import com.madre.excelweb.jpa.entity.VerificationTokenEntity;
import com.madre.excelweb.jpa.repositories.VerificationTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
public class VerificationTokenService {
    private final VerificationTokenRepository verificationTokenRepository;

    @Transactional
    public void saveVerificationToken(EmailVerificationToken token) {
        log.info("Saving verification token for email: {}", token.getEmail());
        verificationTokenRepository.findByEmail(token.getEmail())
                .ifPresent(existing -> verificationTokenRepository.delete(existing));

        final VerificationTokenEntity entity = VerificationTokenEntity.builder()
                .email(token.getEmail())
                .token(token.getToken())
                .expiresAt(OffsetDateTime.now(ZoneOffset.UTC).plusMinutes(EmailVerificationToken.EXPIRATION))
                .build();
        verificationTokenRepository.save(entity);
        log.info("Verification token successfully saved for email: {}", token.getEmail());
    }

    public Optional<EmailVerificationToken> getVerificationToken(String email) {
        return verificationTokenRepository.findByEmail(email)
                .filter(e -> e.getExpiresAt().isAfter(OffsetDateTime.now(ZoneOffset.UTC)))
                .map(e -> EmailVerificationToken.builder()
                        .token(e.getToken())
                        .email(e.getEmail())
                        .expiryDate(e.getExpiresAt().toLocalDateTime())
                        .build());
    }

    @Transactional
    public void removeVerificationToken(String email) {
        log.info("Removing verification token for email: {}", email);
        verificationTokenRepository.deleteByEmail(email);
        log.info("Verification token successfully removed for email: {}", email);
    }

    @Transactional
    @Scheduled(cron = "0 0 * * * *")
    public void cleanupExpired() {
        final int deleted = verificationTokenRepository.deleteExpired(OffsetDateTime.now(ZoneOffset.UTC));
        if (deleted > 0) {
            log.info("Cleaned up {} expired verification tokens", deleted);
        }
    }
}
