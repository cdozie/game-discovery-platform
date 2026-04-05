package com.madre.excelweb.jpa.entity;

import com.madre.excelweb.service.util.PublicIdUtils;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "game_comments", indexes = {
        @Index(name = "game_comments_game_created_idx", columnList = "game_id, created_at"),
        @Index(name = "game_comments_parent_created_idx", columnList = "parent_comment_id, created_at")
})
public class GameComment implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    @Column(name = "public_id", nullable = false, unique = true, updatable = false, length = 64)
    private String publicId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "game_id", nullable = false, updatable = false)
    private GameData game;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_user_id", nullable = false, updatable = false)
    private AppUser author;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_comment_id")
    private GameComment parentComment;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Builder.Default
    @Column(name = "deleted", nullable = false)
    private boolean deleted = false;

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
