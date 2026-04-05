package com.madre.excelweb.service;

import com.madre.excelweb.jpa.repositories.GameRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class GameStatsMaintenanceService {
    private final GameRepository gameRepository;

    @Transactional
    public void rebuildAllGameStats() {
        final int insertedRows = gameRepository.insertMissingGameStatsRows();
        final int refreshedWishlistRows = gameRepository.refreshAllWishlistStats();
        final int refreshedCommentRows = gameRepository.refreshAllCommentCounts();

        log.info(
                "Rebuilt game stats aggregates. insertedMissingRows={}, refreshedWishlistRows={}, refreshedCommentRows={}",
                insertedRows,
                refreshedWishlistRows,
                refreshedCommentRows
        );
    }
}
