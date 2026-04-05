package com.madre.excelweb.jpa.entity;

import com.madre.excelweb.models.MediaPurpose;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Entity
@Table(name = "media_assets", indexes = {
        @Index(name = "media_assets_public_id_idx", columnList = "public_id", unique = true),
        @Index(name = "media_assets_purpose_created_idx", columnList = "purpose, created_at"),
        @Index(name = "media_assets_created_by_created_idx", columnList = "created_by_user_id, created_at")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MediaAsset implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_id", nullable = false, unique = true, length = 64)
    private String publicId;

    @Enumerated(EnumType.STRING)
    @Column(name = "purpose", nullable = false, length = 64)
    private MediaPurpose purpose;

    @Column(name = "content_type", nullable = false, length = 160)
    private String contentType;

    @Column(name = "original_filename")
    private String originalFilename;

    @Column(name = "storage_path", nullable = false, columnDefinition = "TEXT")
    private String storagePath;

    @Column(name = "size_bytes", nullable = false)
    private Long sizeBytes;

    @Column(name = "created_by_user_id")
    private Long createdByUserId;

    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = OffsetDateTime.now(ZoneOffset.UTC);
        }
    }
}
