package com.madre.excelweb.jpa.entity;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.madre.excelweb.service.util.LongToStringSerializer;
import com.madre.excelweb.service.util.PublicIdUtils;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Objects;
import java.util.Set;

@Entity
@Table(name = "games", indexes = {
        @Index(name = "games_name_index", columnList = "name"),
        @Index(name = "games_normalized_title_key_idx", columnList = "normalized_title_key")
})
@SecondaryTable(name = "game_stats", pkJoinColumns = @PrimaryKeyJoinColumn(name = "game_id", referencedColumnName = "id"))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Slf4j
public class GameData implements Serializable {

    @Id
    @Basic(optional = false)
    @Column(name = "id", nullable = false, updatable = false)
    @JsonSerialize(using = LongToStringSerializer.class)
    private Long id;

    @Basic(optional = false)
    @Column(name = "public_id", nullable = false, unique = true, updatable = false, length = 64)
    private String publicId;

    @Basic(optional = false)
    @Column(name = "catalog_sources", nullable = false)
    private String catalogSources;

    @Column(name = "catalog_group_key")
    private String catalogGroupKey;

    @Column(name = "normalized_title_key")
    private String normalizedTitleKey;

    @Basic(optional = false)
    @Column(name = "name", nullable = false)
    private String name;

    @Basic(optional = false)
    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    @Basic(optional = false)
    @Column(name = "categories", nullable = false)
    private String categories;

    @Basic(optional = false)
    @Column(name = "platforms", nullable = false)
    private String platforms;

    @Basic(optional = false)
    @Column(name = "price", nullable = false)
    private String price;

    @Basic(optional = false)
    @Column(name = "url", nullable = false, columnDefinition = "TEXT")
    private String url;

    @Basic(optional = false)
    @Column(name = "release_date", nullable = false)
    private String releaseDate;

    @Basic(optional = false)
    @Column(name = "provider_name", nullable = false)
    private String providerName;

    @Basic(optional = false)
    @Column(name = "image_url", nullable = false, columnDefinition = "TEXT")
    private String imageUrl;

    @Basic(optional = false)
    @Column(name = "admin_locked_fields", nullable = false)
    private String adminLockedFields;

    @Basic
    @Column(name = "admin_last_modified_by_user_id")
    private Long adminLastModifiedByUserId;

    @Basic
    @Column(name = "admin_last_modified_at", columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime adminLastModifiedAt;

    @Basic(optional = false)
    @Column(name = "created", nullable = false, updatable = false, columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime created;

    @Basic(optional = false)
    @Column(name = "last_modified", nullable = false, columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime lastModified;

    @OneToMany(mappedBy = "gameinfo", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Wishlist> wishlists;

    @Basic(optional = false)
    @Column(name = "interactions", nullable = false, columnDefinition = "bigint default 0", table = "game_stats")
    private Long interactions;

    @Basic(optional = false)
    @Column(name = "wishlist_total", nullable = false, columnDefinition = "bigint default 0", table = "game_stats")
    private Long wishlistTotal;

    @Basic(optional = false)
    @Column(name = "comments", nullable = false, columnDefinition = "bigint default 0", table = "game_stats")
    private Long comments;

    @Basic(optional = false)
    @Column(name = "posts", nullable = false, columnDefinition = "bigint default 0", table = "game_stats")
    private Long posts;

    @Basic
    @Column(name = "average_rating", table = "game_stats")
    private Double averageRating;


    public boolean equalsCoreData(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GameData gameData = (GameData) o;

        return Objects.equals(name, gameData.name)
                && Objects.equals(description, gameData.description)
                && Objects.equals(categories, gameData.categories)
                && Objects.equals(platforms, gameData.platforms)
                && Objects.equals(price, gameData.price)
                && Objects.equals(url, gameData.url)
                && Objects.equals(releaseDate, gameData.releaseDate)
                && Objects.equals(providerName, gameData.providerName)
                && Objects.equals(imageUrl, gameData.imageUrl)
                && Objects.equals(normalizedTitleKey, gameData.normalizedTitleKey)
                && Objects.equals(catalogGroupKey, gameData.catalogGroupKey);
    }

    @PrePersist
    public void setDefaultValues() {
        final OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        if (publicId == null || publicId.isBlank()) publicId = PublicIdUtils.newPublicId();
        if (interactions == null) interactions = 0L;
        if (wishlistTotal == null) wishlistTotal = 0L;
        if (comments == null) comments = 0L;
        if (posts == null) posts = 0L;
        if (catalogSources == null || catalogSources.isBlank()) catalogSources = "PLAYSTATION_STORE";
        if (adminLockedFields == null) adminLockedFields = "";
        if (created == null) created = now;
        if (lastModified == null) lastModified = now;
    }

    @PreUpdate
    public void touch() {
        lastModified = OffsetDateTime.now(ZoneOffset.UTC);
    }
}
