package com.madre.excelweb.api.model;

import com.madre.excelweb.models.ListGameStatus;
import lombok.Data;

@Data
public class WishlistEntryRequest {
    ListGameStatus status;
    Double rating;
    Long gameId;
}
