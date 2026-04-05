package com.madre.excelweb.jpa.entity;

import com.madre.excelweb.models.GameSubmissionStatus;
import com.madre.excelweb.service.util.PublicIdUtils;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Entity
@Table(name = "game_submissions", indexes = {
        @Index(name = "game_submissions_status_idx", columnList = "status"),
        @Index(name = "game_submissions_requested_by_idx", columnList = "requested_by_user_id")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GameSubmission implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    @Column(name = "public_id", nullable = false, unique = true, updatable = false, length = 64)
    private String publicId;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "requested_by_user_id", nullable = false)
    private AppUser requestedByUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by_user_id")
    private AppUser reviewedByUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_game_id")
    private GameData createdGame;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private GameSubmissionStatus status;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "categories", columnDefinition = "TEXT")
    private String categories;

    @Column(name = "platforms", columnDefinition = "TEXT")
    private String platforms;

    @Column(name = "provider_name")
    private String providerName;

    @Column(name = "image_url", columnDefinition = "TEXT")
    private String imageUrl;

    @Column(name = "release_date")
    private String releaseDate;

    @Column(name = "url", columnDefinition = "TEXT")
    private String url;

    @Column(name = "review_notes", columnDefinition = "TEXT")
    private String reviewNotes;

    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime createdAt;

    @Column(name = "last_modified", nullable = false, columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime lastModified;

    @PrePersist
    public void prePersist() {
        final OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        if (publicId == null || publicId.isBlank()) {
            publicId = PublicIdUtils.newPublicId();
        }
        if (status == null) {
            status = GameSubmissionStatus.PENDING;
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
}
