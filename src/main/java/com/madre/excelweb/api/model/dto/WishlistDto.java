package com.madre.excelweb.api.model.dto;

import com.madre.excelweb.models.ListGameStatus;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class WishlistDto {
    private Long id;
    private GameDto gameinfo;
    private Double rating;
    private ListGameStatus status;
}
