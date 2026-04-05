package com.madre.excelweb.api.model;

import lombok.Data;

@Data
public class WishlistRequest {
    private String gameId;
    private Double rating;
    private String status;
    private String selectedPlatform;
}
