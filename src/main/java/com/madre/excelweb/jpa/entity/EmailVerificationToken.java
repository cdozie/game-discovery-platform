package com.madre.excelweb.jpa.entity;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(name = "email_verification_token")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor

public class EmailVerificationToken implements Serializable {

    public static final int EXPIRATION = 24 * 60;  // 24 hours

    @Id
    @Basic(optional = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Basic(optional = false)
    @Column(name = "token", nullable = false, updatable = false)
    private String token;

    @Basic(optional = false)
    @Column(name = "email", nullable = false, updatable = false)
    private String email;

    @Basic(optional = false)
    @Column(name = "expiry_date", nullable = false, updatable = false)
    private LocalDateTime expiryDate;


    public static LocalDateTime calculateExpiryDate(int expiryTimeInMinutes) {
        return LocalDateTime.now().plusMinutes(expiryTimeInMinutes);
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(this.expiryDate);
    }
}
