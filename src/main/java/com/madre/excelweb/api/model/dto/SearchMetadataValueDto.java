package com.madre.excelweb.api.model.dto;

import com.madre.excelweb.jpa.repositories.SearchMetadataValueProjection;

public record SearchMetadataValueDto(
        String value,
        long count
) {
    public static SearchMetadataValueDto fromProjection(final SearchMetadataValueProjection projection) {
        return new SearchMetadataValueDto(projection.getValue(), projection.getCount());
    }
}
