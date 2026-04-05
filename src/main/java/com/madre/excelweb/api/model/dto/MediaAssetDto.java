package com.madre.excelweb.api.model.dto;

import com.madre.excelweb.jpa.entity.MediaAsset;

import java.time.OffsetDateTime;

public record MediaAssetDto(
        String publicId,
        String purpose,
        String contentType,
        String originalFilename,
        long sizeBytes,
        String url,
        OffsetDateTime createdAt
) {
    public static MediaAssetDto fromEntity(final MediaAsset mediaAsset, final String url) {
        return new MediaAssetDto(
                mediaAsset.getPublicId(),
                mediaAsset.getPurpose().name(),
                mediaAsset.getContentType(),
                mediaAsset.getOriginalFilename(),
                mediaAsset.getSizeBytes() == null ? 0L : mediaAsset.getSizeBytes(),
                url,
                mediaAsset.getCreatedAt()
        );
    }
}
