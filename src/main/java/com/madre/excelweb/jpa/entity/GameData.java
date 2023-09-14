package com.madre.excelweb.jpa.entity;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.madre.excelweb.service.util.LongToStringSerializer;
import jakarta.persistence.Basic;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.Set;

@Entity
@Table(name = "games", uniqueConstraints = {@UniqueConstraint(columnNames = {"id"})})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GameData implements Serializable {

    @Basic(optional = false)
    @Column(name = "id", nullable = false, updatable = false)
    @JsonSerialize(using = LongToStringSerializer.class)
    private Long id;

    @Id
    @Basic(optional = false)
    @Column(name = "psn_id", nullable = false, updatable = false)
    private String psnId;

    @Basic(optional = false)
    @Column(name = "name", nullable = false, updatable = false)
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
    @Column(name = "url", nullable = false)
    private String url;

    @Basic(optional = false)
    @Column(name = "release_date", nullable = false)
    private String release_date;

    @Basic(optional = false)
    @Column(name = "providerName", nullable = false, updatable = false)
    private String providerName;

    @Basic(optional = false)
    @Column(name = "image_url", nullable = false)
    private String imageUrl;

    @Basic(optional = false)
    @Column(name = "created", nullable = false, updatable = false, columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime created;

    @Basic(optional = false)
    @Column(name = "last_modified", nullable = false, columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime lastModified;

    @OneToMany(mappedBy = "gameinfo", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Wishlist> wishlists;

    public boolean equalsCoreData(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GameData gameData = (GameData) o;

        return Objects.equals(psnId, gameData.psnId)
                && Objects.equals(name, gameData.name)
                && Objects.equals(description, gameData.description)
                && Objects.equals(categories, gameData.categories)
                && Objects.equals(platforms, gameData.platforms)
                && Objects.equals(price, gameData.price)
                && Objects.equals(url, gameData.url)
                && Objects.equals(release_date, gameData.release_date)
                && Objects.equals(providerName, gameData.providerName)
                && Objects.equals(imageUrl, gameData.imageUrl);
    }


}
