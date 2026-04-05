package com.madre.excelweb.jpa.repositories;

import com.madre.excelweb.jpa.entity.VerificationTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.Optional;

@Repository
public interface VerificationTokenRepository extends JpaRepository<VerificationTokenEntity, Long> {

    Optional<VerificationTokenEntity> findByEmail(String email);

    void deleteByEmail(String email);

    @Modifying
    @Query("DELETE FROM VerificationTokenEntity v WHERE v.expiresAt < :now")
    int deleteExpired(OffsetDateTime now);
}
