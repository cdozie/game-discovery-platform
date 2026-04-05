package com.madre.excelweb.service.api;

import com.madre.excelweb.api.model.CatalogSyncRequest;
import com.madre.excelweb.jpa.entity.CatalogSyncRun;
import com.madre.excelweb.jpa.repositories.CatalogSyncRunRepository;
import com.madre.excelweb.models.CatalogSyncStatus;
import com.madre.excelweb.models.CatalogSource;
import com.madre.excelweb.service.CatalogBatchUpsertResult;
import com.madre.excelweb.service.CatalogUpsertService;
import com.madre.excelweb.service.RedissonLockClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.retry.support.RetryTemplate;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@RequiredArgsConstructor
public class PsnGameSyncService {
    private static final CatalogSource CATALOG_SOURCE = CatalogSource.PLAYSTATION_STORE;
    private static final String DEFAULT_PSN_SYNC_PATH = "catalog-sync";
    private static final String PSN_SYNC_PATH = "psn-sync.lock-path";
    private static final String PSN_SYNC_ENABLED = "web.app.psn-sync.enabled";
    private static final String CATALOG_MAINTENANCE_MODE = "web.app.catalog.maintenance-mode";
    private static final String PSN_SYNC_CRON = "${web.app.psn-sync.cron:0 0 0 * * ?}";
    private static final String PSN_SYNC_PAGE_SIZE = "web.app.psn-sync.page-size";
    private static final int DEFAULT_PSN_SYNC_PAGE_SIZE = 250;
    private final PsnApiReader psnApiReader;
    private final RetryTemplate iteratorRetryTemplate;
    private final CatalogUpsertService catalogUpsertService;
    private final CatalogSyncRunRepository catalogSyncRunRepository;
    private final RedissonLockClient redissonLockClient;
    private final Environment env;
    private final Executor catalogSyncExecutor;
    private final AtomicBoolean syncInProgress = new AtomicBoolean(false);

    @Scheduled(cron = PSN_SYNC_CRON)
    public void scheduledSyncGames() {
        if (isCatalogMaintenanceActive()) {
            log.info("Skipping scheduled PSN sync because catalog maintenance mode is active.");
            return;
        }
        triggerSync("SCHEDULED", null);
    }

    public CatalogSyncRun triggerManualSync(final CatalogSyncRequest request) {
        return triggerSync("MANUAL", request);
    }

    public CatalogSyncRun triggerManualSync(final String source, final CatalogSyncRequest request) {
        return triggerSync(source, request);
    }

    public CatalogSyncRun triggerStartupSync() {
        return triggerSync("STARTUP", null);
    }

    public Optional<CatalogSyncRun> getLatestRun() {
        return catalogSyncRunRepository.findTopByCatalogSourceOrderByStartedAtDesc(CATALOG_SOURCE);
    }

    public boolean isSyncInProgress() {
        return syncInProgress.get();
    }

    private boolean isCatalogMaintenanceActive() {
        return "consolidate".equalsIgnoreCase(env.getProperty(CATALOG_MAINTENANCE_MODE, ""));
    }

    public CatalogSyncRun triggerSync(final String source, final CatalogSyncRequest request) {
        if (!env.getProperty(PSN_SYNC_ENABLED, Boolean.class, true)) {
            return saveImmediateRun(source, CatalogSyncStatus.SKIPPED, request, "PSN catalog sync is disabled.");
        }

        final Optional<CatalogSyncRun> runningRun = catalogSyncRunRepository.findTopByStatusOrderByStartedAtDesc(CatalogSyncStatus.RUNNING);
        if (runningRun.isPresent()) {
            return runningRun.get();
        }

        final CatalogSyncRequest normalizedRequest = normalizeRequest(request);
        final int startOffset = resolveStartOffset(normalizedRequest);
        final CatalogSyncRun createdRun = catalogSyncRunRepository.save(CatalogSyncRun.builder()
                .status(CatalogSyncStatus.RUNNING)
                .catalogSource(CATALOG_SOURCE)
                .triggeredBy(source)
                .startedAt(OffsetDateTime.now(ZoneOffset.UTC))
                .requestedPageSize(normalizedRequest.pageSize())
                .requestedMaxBatches(normalizedRequest.maxBatches())
                .requestedMaxRecords(normalizedRequest.maxRecords())
                .requestedStartOffset(startOffset)
                .currentOffset(startOffset)
                .completedBatches(0)
                .processedCount(0L)
                .createdCount(0L)
                .updatedCount(0L)
                .unchangedCount(0L)
                .failedCount(0L)
                .build());

        if (!syncInProgress.compareAndSet(false, true)) {
            return completeRun(createdRun.getId(), CatalogSyncStatus.SKIPPED, normalizedRequest, "Another sync is already running.");
        }

        catalogSyncExecutor.execute(() -> executeSync(createdRun.getId(), normalizedRequest));
        return createdRun;
    }

    private void executeSync(final Long runId, final CatalogSyncRequest request) {
        try {
            final boolean executed = redissonLockClient.run(env.getProperty(PSN_SYNC_PATH, DEFAULT_PSN_SYNC_PATH),
                    () -> syncGamesInternal(runId, request));
            if (!executed) {
                completeRun(runId, CatalogSyncStatus.SKIPPED, request, "Could not acquire the Redis sync lock.");
            }
        } catch (Exception e) {
            log.error("Error executing PSN sync run {}", runId, e);
            completeRunFromCurrentState(runId, CatalogSyncStatus.FAILED, request, summarizeException(e));
        } finally {
            syncInProgress.set(false);
        }
    }

    private void syncGamesInternal(final Long runId, final CatalogSyncRequest request) {
        final SyncCounters syncCounters = loadSyncCounters(runId);
        final long startTime = System.currentTimeMillis();
        int totalResults = Integer.MAX_VALUE;

        try {
            while (syncCounters.currentOffset < totalResults && !hasReachedRequestLimit(syncCounters, request)) {
                final int requestedPageSize = resolveRequestedPageSize(request, syncCounters);
                final int batchOffset = syncCounters.currentOffset;
                log.info("Starting sync of batch #{} at offset {} with page size {}",
                        syncCounters.completedBatches + 1, batchOffset, requestedPageSize);

                final PsnGameBatch gameBatch = iteratorRetryTemplate.execute(context ->
                        psnApiReader.getGamesPage(requestedPageSize, batchOffset));

                totalResults = gameBatch.totalResults();

                if (gameBatch.games().isEmpty()) {
                    break;
                }

                final CatalogBatchUpsertResult upsertResult = catalogUpsertService.upsertBatch(gameBatch.games(), CATALOG_SOURCE);
                syncCounters.processedCount += upsertResult.processedCount();
                syncCounters.createdCount += upsertResult.createdCount();
                syncCounters.updatedCount += upsertResult.updatedCount();
                syncCounters.unchangedCount += upsertResult.unchangedCount();
                syncCounters.currentOffset += gameBatch.games().size();
                syncCounters.completedBatches++;
                saveCheckpoint(runId, request, syncCounters, checkpointMessage("PlayStation", syncCounters));
                log.info("Successfully synced batch #{}", syncCounters.completedBatches);

                if (gameBatch.games().size() < requestedPageSize) {
                    break;
                }
            }
        } catch (Exception e) {
            syncCounters.failedCount++;
            final String failureMessage = "PSN sync failed at offset %s after %s batches. %s"
                    .formatted(syncCounters.currentOffset, syncCounters.completedBatches, summarizeException(e));
            log.error("Error syncing PSN batch at offset {}", syncCounters.currentOffset, e);
            completeRun(runId, CatalogSyncStatus.FAILED, request, failureMessage, syncCounters);
            return;
        }

        final long totalTime = System.currentTimeMillis() - startTime;
        final String completionMessage = completionMessageGenerator(syncCounters, totalTime);
        completeRun(runId, CatalogSyncStatus.SUCCESS, request, completionMessage, syncCounters);
        log.info(completionMessage);
    }

    private boolean hasReachedRequestLimit(final SyncCounters syncCounters,
                                           final CatalogSyncRequest request) {
        final boolean maxBatchesReached = request.maxBatches() != null && syncCounters.completedBatches >= request.maxBatches();
        final boolean maxRecordsReached = request.maxRecords() != null && syncCounters.processedCount >= request.maxRecords();
        return maxBatchesReached || maxRecordsReached;
    }

    private int resolveRequestedPageSize(final CatalogSyncRequest request, final SyncCounters syncCounters) {
        if (request.maxRecords() == null) {
            return request.pageSize();
        }

        final int remaining = Math.max(0, request.maxRecords() - Math.toIntExact(syncCounters.processedCount));
        return Math.min(request.pageSize(), remaining);
    }

    private String completionMessageGenerator(final SyncCounters syncCounters, final long totalTime) {
        return "Finished PSN sync in %s ms across %s batches. Processed=%s, created=%s, updated=%s, unchanged=%s, failed=%s"
                .formatted(totalTime, syncCounters.completedBatches, syncCounters.processedCount, syncCounters.createdCount,
                        syncCounters.updatedCount, syncCounters.unchangedCount, syncCounters.failedCount);
    }

    private CatalogSyncRequest normalizeRequest(final CatalogSyncRequest request) {
        final int configuredPageSize = env.getProperty(PSN_SYNC_PAGE_SIZE, Integer.class, DEFAULT_PSN_SYNC_PAGE_SIZE);
        final int pageSize = clamp(request == null ? null : request.pageSize(), 1, 512, configuredPageSize);
        final Integer maxBatches = normalizeNullablePositive(request == null ? null : request.maxBatches());
        final Integer maxRecords = normalizeNullablePositive(request == null ? null : request.maxRecords());
        final Integer startOffset = normalizeNullableOffset(request == null ? null : request.startOffset());
        return new CatalogSyncRequest(CATALOG_SOURCE.name(), pageSize, maxBatches, maxRecords, startOffset);
    }

    private int clamp(final Integer value, final int min, final int max, final int fallback) {
        final int resolved = value == null ? fallback : value;
        return Math.max(min, Math.min(max, resolved));
    }

    private Integer normalizeNullablePositive(final Integer value) {
        if (value == null || value <= 0) {
            return null;
        }
        return value;
    }

    private Integer normalizeNullableOffset(final Integer value) {
        if (value == null) {
            return null;
        }
        return Math.max(0, value);
    }

    private int resolveStartOffset(final CatalogSyncRequest request) {
        if (request.startOffset() != null) {
            return request.startOffset();
        }

        return catalogSyncRunRepository.findTopByCatalogSourceOrderByStartedAtDesc(CATALOG_SOURCE)
                .filter(run -> run.getStatus() == CatalogSyncStatus.FAILED)
                .map(CatalogSyncRun::getCurrentOffset)
                .filter(offset -> offset != null && offset >= 0)
                .orElse(0);
    }

    private CatalogSyncRun saveImmediateRun(final String source,
                                            final CatalogSyncStatus status,
                                            final CatalogSyncRequest request,
                                            final String message) {
        final CatalogSyncRequest normalizedRequest = normalizeRequest(request);
        final int startOffset = normalizedRequest.startOffset() == null ? 0 : normalizedRequest.startOffset();
        return catalogSyncRunRepository.save(CatalogSyncRun.builder()
                .status(status)
                .catalogSource(CATALOG_SOURCE)
                .triggeredBy(source)
                .startedAt(OffsetDateTime.now(ZoneOffset.UTC))
                .completedAt(OffsetDateTime.now(ZoneOffset.UTC))
                .requestedPageSize(normalizedRequest.pageSize())
                .requestedMaxBatches(normalizedRequest.maxBatches())
                .requestedMaxRecords(normalizedRequest.maxRecords())
                .requestedStartOffset(startOffset)
                .currentOffset(startOffset)
                .completedBatches(0)
                .processedCount(0L)
                .createdCount(0L)
                .updatedCount(0L)
                .unchangedCount(0L)
                .failedCount(0L)
                .message(message)
                .build());
    }

    private CatalogSyncRun completeRun(final Long runId,
                                       final CatalogSyncStatus status,
                                       final CatalogSyncRequest request,
                                       final String message) {
        return completeRunFromCurrentState(runId, status, request, message);
    }

    private CatalogSyncRun completeRun(final Long runId,
                                       final CatalogSyncStatus status,
                                       final CatalogSyncRequest request,
                                       final String message,
                                       final SyncCounters syncCounters) {
        final CatalogSyncRun run = catalogSyncRunRepository.findById(runId)
                .orElseThrow(() -> new IllegalStateException("Sync run %s was not found".formatted(runId)));
        final CatalogSyncRequest normalizedRequest = normalizeRequest(request);

        run.setStatus(status);
        run.setCatalogSource(CATALOG_SOURCE);
        run.setCompletedAt(OffsetDateTime.now(ZoneOffset.UTC));
        run.setRequestedPageSize(normalizedRequest.pageSize());
        run.setRequestedMaxBatches(normalizedRequest.maxBatches());
        run.setRequestedMaxRecords(normalizedRequest.maxRecords());
        if (run.getRequestedStartOffset() == null) {
            run.setRequestedStartOffset(resolveStartOffset(normalizedRequest));
        }
        run.setCurrentOffset(syncCounters.currentOffset);
        run.setCompletedBatches(syncCounters.completedBatches);
        run.setLastCheckpointAt(OffsetDateTime.now(ZoneOffset.UTC));
        run.setProcessedCount(syncCounters.processedCount);
        run.setCreatedCount(syncCounters.createdCount);
        run.setUpdatedCount(syncCounters.updatedCount);
        run.setUnchangedCount(syncCounters.unchangedCount);
        run.setFailedCount(syncCounters.failedCount);
        run.setMessage(message);
        return catalogSyncRunRepository.save(run);
    }

    private CatalogSyncRun completeRunFromCurrentState(final Long runId,
                                                       final CatalogSyncStatus status,
                                                       final CatalogSyncRequest request,
                                                       final String message) {
        final SyncCounters syncCounters = loadSyncCounters(runId);
        return completeRun(runId, status, request, message, syncCounters);
    }

    private void saveCheckpoint(final Long runId,
                                final CatalogSyncRequest request,
                                final SyncCounters syncCounters,
                                final String message) {
        final CatalogSyncRun run = catalogSyncRunRepository.findById(runId)
                .orElseThrow(() -> new IllegalStateException("Sync run %s was not found".formatted(runId)));
        final CatalogSyncRequest normalizedRequest = normalizeRequest(request);

        run.setStatus(CatalogSyncStatus.RUNNING);
        run.setCatalogSource(CATALOG_SOURCE);
        run.setRequestedPageSize(normalizedRequest.pageSize());
        run.setRequestedMaxBatches(normalizedRequest.maxBatches());
        run.setRequestedMaxRecords(normalizedRequest.maxRecords());
        if (run.getRequestedStartOffset() == null) {
            run.setRequestedStartOffset(resolveStartOffset(normalizedRequest));
        }
        run.setCurrentOffset(syncCounters.currentOffset);
        run.setCompletedBatches(syncCounters.completedBatches);
        run.setLastCheckpointAt(OffsetDateTime.now(ZoneOffset.UTC));
        run.setProcessedCount(syncCounters.processedCount);
        run.setCreatedCount(syncCounters.createdCount);
        run.setUpdatedCount(syncCounters.updatedCount);
        run.setUnchangedCount(syncCounters.unchangedCount);
        run.setFailedCount(syncCounters.failedCount);
        run.setMessage(message);
        catalogSyncRunRepository.save(run);
    }

    private SyncCounters loadSyncCounters(final Long runId) {
        final CatalogSyncRun run = catalogSyncRunRepository.findById(runId)
                .orElseThrow(() -> new IllegalStateException("Sync run %s was not found".formatted(runId)));
        final SyncCounters syncCounters = new SyncCounters();
        syncCounters.currentOffset = run.getCurrentOffset() == null ? 0 : run.getCurrentOffset();
        syncCounters.completedBatches = run.getCompletedBatches() == null ? 0 : run.getCompletedBatches();
        syncCounters.processedCount = run.getProcessedCount() == null ? 0L : run.getProcessedCount();
        syncCounters.createdCount = run.getCreatedCount() == null ? 0L : run.getCreatedCount();
        syncCounters.updatedCount = run.getUpdatedCount() == null ? 0L : run.getUpdatedCount();
        syncCounters.unchangedCount = run.getUnchangedCount() == null ? 0L : run.getUnchangedCount();
        syncCounters.failedCount = run.getFailedCount() == null ? 0L : run.getFailedCount();
        return syncCounters;
    }

    private String checkpointMessage(final String sourceLabel, final SyncCounters syncCounters) {
        return "%s checkpoint at offset %s after %s batches. Processed=%s, created=%s, updated=%s, unchanged=%s, failed=%s"
                .formatted(sourceLabel, syncCounters.currentOffset, syncCounters.completedBatches, syncCounters.processedCount,
                        syncCounters.createdCount, syncCounters.updatedCount, syncCounters.unchangedCount, syncCounters.failedCount);
    }

    private String summarizeException(final Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }

        final String message = current.getMessage();
        if (message != null && !message.isBlank()) {
            return message;
        }
        return current.getClass().getSimpleName();
    }

    private static final class SyncCounters {
        private int currentOffset;
        private int completedBatches;
        private long processedCount;
        private long createdCount;
        private long updatedCount;
        private long unchangedCount;
        private long failedCount;
    }

}
