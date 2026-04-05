package com.madre.excelweb.api.model.dto;

import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
@Builder
public class GameDto {
    private String id;
    private String catalogSources;
    private String name;
    private String description;
    private String categories;
    private String platforms;
    private String price;
    private String url;
    private String releaseDate;
    private String provider;
    private String imageUrl;
    private String interactions;
    private String comments;
    private String wishlistTotal;
    private String averageRating;
    private OffsetDateTime created;
    private OffsetDateTime lastModified;
    private Boolean inWishlist;

}
