//package com.madre.excelweb.jpa.repositories;
//
//import com.madre.excelweb.jpa.entity.EmailVerificationToken;
//import jakarta.persistence.Table;
//import org.springframework.data.jpa.repository.JpaRepository;
//import org.springframework.stereotype.Repository;
//
//import java.util.Optional;
//
//@Repository
//@Table(name = "email_verification_tokens")
//public interface VerificationTokenRepository extends JpaRepository<EmailVerificationToken, Long> {
//    Optional<EmailVerificationToken> findByToken(String token);
//
//
//}
