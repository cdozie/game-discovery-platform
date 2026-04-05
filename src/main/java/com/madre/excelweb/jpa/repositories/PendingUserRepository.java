package com.madre.excelweb.jpa.repositories;

import com.madre.excelweb.jpa.entity.PendingUserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.Optional;

@Repository
public interface PendingUserRepository extends JpaRepository<PendingUserEntity, Long> {

    Optional<PendingUserEntity> findByEmail(String email);

    void deleteByEmail(String email);

    @Modifying
    @Query("DELETE FROM PendingUserEntity p WHERE p.expiresAt < :now")
    int deleteExpired(OffsetDateTime now);
}
