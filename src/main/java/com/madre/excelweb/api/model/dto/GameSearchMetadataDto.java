package com.madre.excelweb.api.model.dto;

import java.util.List;

public record GameSearchMetadataDto(
        List<SearchMetadataValueDto> publishers,
        List<SearchMetadataValueDto> platforms,
        List<SearchMetadataValueDto> categories,
        List<SearchMetadataValueDto> catalogSources
) {
}
