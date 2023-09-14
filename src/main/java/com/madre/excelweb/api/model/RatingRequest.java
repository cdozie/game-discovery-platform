package com.madre.excelweb.api.model;

import lombok.Data;

@Data
public class RatingRequest {
    private Long gameId;
    private Long userId;
    private Long rating;
}
