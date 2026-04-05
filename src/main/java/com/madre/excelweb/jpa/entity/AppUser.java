package com.madre.excelweb.jpa.entity;

import com.madre.excelweb.service.util.PublicIdUtils;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Set;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "users", uniqueConstraints = {
        @UniqueConstraint(columnNames = "username"),
        @UniqueConstraint(columnNames = "email")
}
)

public class AppUser implements Serializable {
    @Id
    @Basic(optional = false)
    @Column(name = "id", nullable = false, updatable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Basic(optional = false)
    @Column(name = "public_id", nullable = false, unique = true, updatable = false, length = 64)
    private String publicId;

    @Basic(optional = false)
    @Column(name = "username", nullable = false, updatable = false)
    private String username;

    @Basic(optional = false)
    @Column(name = "email", nullable = false, updatable = false)
    private String email;


    @Basic(optional = false)
    @Column(name = "password", nullable = false)
    private String password;

    @Basic
    @Column(name = "display_name")
    private String displayName;

    @Basic
    @Column(name = "bio", columnDefinition = "TEXT")
    private String bio;

    @Basic
    @Column(name = "avatar_url", columnDefinition = "TEXT")
    private String avatarUrl;

    @Basic
    @Column(name = "favorite_platform")
    private String favoritePlatform;

    @Basic
    @Column(name = "ban_reason", columnDefinition = "TEXT")
    private String banReason;

    @Basic
    @Column(name = "ban_started_at", columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime banStartedAt;

    @Basic
    @Column(name = "ban_expires_at", columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime banExpiresAt;

    @Basic(optional = false)
    @Column(name = "ban_permanent", nullable = false)
    private boolean banPermanent;

    @Basic
    @Column(name = "banned_by_user_id")
    private Long bannedByUserId;

    @Basic(optional = false)
    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime createdAt;

    @Basic(optional = false)
    @Column(name = "last_modified", nullable = false, columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime lastModified;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id", referencedColumnName = "id", nullable = false, updatable = false),
            inverseJoinColumns = @JoinColumn(name = "role_id", referencedColumnName = "id", nullable = false, updatable = false))
    private Set<AppRole> roles;

    @PrePersist
    public void prePersist() {
        final OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        if (publicId == null || publicId.isBlank()) {
            publicId = PublicIdUtils.newPublicId();
        }
        if (displayName == null || displayName.isBlank()) {
            displayName = username;
        }
        if (createdAt == null) {
            createdAt = now;
        }
        if (lastModified == null) {
            lastModified = now;
        }
    }

    @PreUpdate
    public void preUpdate() {
        lastModified = OffsetDateTime.now(ZoneOffset.UTC);
    }

    public boolean isCurrentlyBanned() {
        if (banStartedAt == null) {
            return false;
        }

        if (banPermanent) {
            return true;
        }

        if (banExpiresAt == null) {
            return false;
        }

        return banExpiresAt.isAfter(OffsetDateTime.now(ZoneOffset.UTC));
    }

}
