package com.madre.excelweb.api.model.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GameDataInteractionsDto {
    private String id;
    private String psnId;
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
    private Boolean inWishlist;
}
