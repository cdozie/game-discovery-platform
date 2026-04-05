package com.madre.excelweb.service;

import com.madre.excelweb.api.model.CatalogSyncRequest;
import com.madre.excelweb.jpa.entity.CatalogSyncRun;
import com.madre.excelweb.jpa.repositories.CatalogSyncRunRepository;
import com.madre.excelweb.models.CatalogSyncStatus;
import com.madre.excelweb.models.CatalogSource;
import com.madre.excelweb.service.api.IgdbGameSyncService;
import com.madre.excelweb.service.api.PsnGameSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.time.OffsetDateTime;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CatalogSyncCoordinator {
    private static final int DEFAULT_STALE_SYNC_THRESHOLD_MINUTES = 30;

    private final PsnGameSyncService psnGameSyncService;
    private final IgdbGameSyncService igdbGameSyncService;
    private final CatalogSyncRunRepository catalogSyncRunRepository;

    public CatalogSyncRun triggerSync(final CatalogSyncRequest request) {
        return triggerSync("MANUAL", request);
    }

    public CatalogSyncRun triggerSync(final String triggerSource, final CatalogSyncRequest request) {
        final CatalogSource catalogSource = CatalogSource.fromNullable(request == null ? null : request.catalogSource());
        return switch (catalogSource) {
            case PLAYSTATION_STORE -> psnGameSyncService.triggerManualSync(triggerSource, request);
            case IGDB_PC_XBOX -> igdbGameSyncService.triggerManualSync(triggerSource, request);
            case USER_SUBMITTED -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "User-submitted catalog entries are managed through the admin workflow, not catalog sync.");
        };
    }

    public Optional<CatalogSyncRun> getLatestRun() {
        return catalogSyncRunRepository.findTopByOrderByStartedAtDesc();
    }

    public Optional<CatalogSyncRun> getLatestRun(final CatalogSource catalogSource) {
        return catalogSyncRunRepository.findTopByCatalogSourceOrderByStartedAtDesc(catalogSource);
    }

    public boolean isAnySyncInProgress() {
        return psnGameSyncService.isSyncInProgress() || igdbGameSyncService.isSyncInProgress();
    }

    public void triggerStartupSyncsIfConfigured() {
        psnGameSyncService.triggerStartupSync();
        if (igdbGameSyncService.isConfiguredAndEnabled()) {
            igdbGameSyncService.triggerStartupSync();
        }
    }

    public void failStaleRunningSyncs() {
        final OffsetDateTime staleThreshold = OffsetDateTime.now().minusMinutes(DEFAULT_STALE_SYNC_THRESHOLD_MINUTES);
        final var staleRuns = catalogSyncRunRepository.findAllByStatusOrderByStartedAtAsc(CatalogSyncStatus.RUNNING).stream()
                .filter(run -> run.getStartedAt() != null && run.getStartedAt().isBefore(staleThreshold))
                .toList();

        if (staleRuns.isEmpty()) {
            return;
        }

        staleRuns.forEach(run -> {
            run.setStatus(CatalogSyncStatus.FAILED);
            run.setCompletedAt(OffsetDateTime.now());
            run.setMessage("Marked failed on startup because the previous sync process did not complete cleanly.");
        });
        catalogSyncRunRepository.saveAll(staleRuns);
        log.warn("Marked {} stale catalog sync runs as failed during startup cleanup.", staleRuns.size());
    }
}
