package com.madre.excelweb.jpa.entity;

import com.madre.excelweb.models.CatalogSource;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Entity
@Table(name = "game_source_entries", uniqueConstraints = {
        @UniqueConstraint(name = "uq_game_source_entries_source_external", columnNames = {"catalog_source", "external_id"})
}, indexes = {
        @Index(name = "game_source_entries_game_id_idx", columnList = "game_id")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GameSourceEntry implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "game_id", nullable = false)
    private GameData game;

    @Enumerated(EnumType.STRING)
    @Column(name = "catalog_source", nullable = false, length = 64)
    private CatalogSource catalogSource;

    @Column(name = "external_id", nullable = false)
    private String externalId;

    @Column(name = "created", nullable = false, updatable = false, columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime created;

    @Column(name = "last_modified", nullable = false, columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime lastModified;

    @PrePersist
    public void setDefaultValues() {
        final OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        if (created == null) {
            created = now;
        }
        if (lastModified == null) {
            lastModified = now;
        }
    }

    @PreUpdate
    public void touch() {
        lastModified = OffsetDateTime.now(ZoneOffset.UTC);
    }
}
