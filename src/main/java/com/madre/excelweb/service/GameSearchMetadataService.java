package com.madre.excelweb.service;

import com.madre.excelweb.api.model.dto.GameSearchMetadataDto;
import com.madre.excelweb.api.model.dto.SearchMetadataValueDto;
import com.madre.excelweb.jpa.repositories.GameRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GameSearchMetadataService {
    private final GameRepository gameRepository;

    @Transactional(readOnly = true)
    public GameSearchMetadataDto getMetadata(final String publisherQuery,
                                             final String platformQuery,
                                             final String categoryQuery,
                                             final Integer limit) {
        final int resolvedLimit = Math.max(1, Math.min(limit == null ? 12 : limit, 25));
        return new GameSearchMetadataDto(
                gameRepository.searchPublisherMetadata(normalizePrefix(publisherQuery), resolvedLimit).stream()
                        .map(SearchMetadataValueDto::fromProjection)
                        .toList(),
                gameRepository.searchPlatformMetadata(normalizePrefix(platformQuery), resolvedLimit).stream()
                        .map(SearchMetadataValueDto::fromProjection)
                        .toList(),
                gameRepository.searchCategoryMetadata(normalizePrefix(categoryQuery), resolvedLimit).stream()
                        .map(SearchMetadataValueDto::fromProjection)
                        .toList(),
                gameRepository.searchCatalogSourceMetadata(null, Math.min(resolvedLimit, 8)).stream()
                        .map(SearchMetadataValueDto::fromProjection)
                        .toList()
        );
    }

    private String normalizePrefix(final String rawValue) {
        if (rawValue == null) {
            return null;
        }
        final String normalized = rawValue.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
