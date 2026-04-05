package com.madre.excelweb.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Entity
@Table(name = "admin_catalog_settings")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminCatalogSettings implements Serializable {
    public static final long SINGLETON_ID = 1L;

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "spotlight_game_id")
    private GameData spotlightGame;

    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime createdAt;

    @Column(name = "last_modified", nullable = false, columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime lastModified;

    @PrePersist
    public void prePersist() {
        final OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        if (id == null) {
            id = SINGLETON_ID;
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
