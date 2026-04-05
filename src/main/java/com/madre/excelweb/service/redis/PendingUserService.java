package com.madre.excelweb.service.redis;

import com.madre.excelweb.jpa.entity.AppUser;
import com.madre.excelweb.jpa.entity.EmailVerificationToken;
import com.madre.excelweb.jpa.entity.PendingUserEntity;
import com.madre.excelweb.jpa.repositories.PendingUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
public class PendingUserService {
    private final PendingUserRepository pendingUserRepository;

    @Transactional
    public void savePendingUser(AppUser appUser) {
        log.info("Saving pending user: {}", appUser.getUsername());
        pendingUserRepository.findByEmail(appUser.getEmail())
                .ifPresent(existing -> pendingUserRepository.delete(existing));

        final PendingUserEntity entity = PendingUserEntity.builder()
                .email(appUser.getEmail())
                .username(appUser.getUsername())
                .passwordHash(appUser.getPassword())
                .expiresAt(OffsetDateTime.now(ZoneOffset.UTC).plusMinutes(EmailVerificationToken.EXPIRATION))
                .build();
        pendingUserRepository.save(entity);
        log.info("Pending user successfully saved: {}", appUser.getUsername());
    }

    public Optional<AppUser> getPendingUser(String email) {
        return pendingUserRepository.findByEmail(email)
                .filter(e -> e.getExpiresAt().isAfter(OffsetDateTime.now(ZoneOffset.UTC)))
                .map(e -> AppUser.builder()
                        .username(e.getUsername())
                        .email(e.getEmail())
                        .password(e.getPasswordHash())
                        .build());
    }

    @Transactional
    public void removePendingUser(String email) {
        log.info("Removing pending user: {}", email);
        pendingUserRepository.deleteByEmail(email);
        log.info("Pending user successfully removed: {}", email);
    }

    @Transactional
    @Scheduled(cron = "0 0 * * * *")
    public void cleanupExpired() {
        final int deleted = pendingUserRepository.deleteExpired(OffsetDateTime.now(ZoneOffset.UTC));
        if (deleted > 0) {
            log.info("Cleaned up {} expired pending users", deleted);
        }
    }
}
