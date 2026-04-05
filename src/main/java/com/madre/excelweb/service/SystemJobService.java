package com.madre.excelweb.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.madre.excelweb.api.model.CatalogSyncRequest;
import com.madre.excelweb.api.model.CreateSystemJobRequest;
import com.madre.excelweb.api.model.dto.CatalogSyncRunDto;
import com.madre.excelweb.api.model.dto.SystemJobDto;
import com.madre.excelweb.jpa.entity.AppUser;
import com.madre.excelweb.jpa.entity.CatalogSyncRun;
import com.madre.excelweb.jpa.entity.SystemJob;
import com.madre.excelweb.jpa.repositories.CatalogSyncRunRepository;
import com.madre.excelweb.jpa.repositories.SystemJobRepository;
import com.madre.excelweb.models.CatalogSyncStatus;
import com.madre.excelweb.models.SystemJobStatus;
import com.madre.excelweb.models.SystemJobType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class SystemJobService {
    private static final int DEFAULT_JOB_LIMIT = 40;
    private static final int DEFAULT_STALE_JOB_THRESHOLD_MINUTES = 60;

    private final SystemJobRepository systemJobRepository;
    private final CatalogSyncRunRepository catalogSyncRunRepository;
    private final CatalogSyncCoordinator catalogSyncCoordinator;
    private final CatalogConsolidationService catalogConsolidationService;
    private final GameStatsMaintenanceService gameStatsMaintenanceService;
    private final ObjectMapper objectMapper;
    @Qualifier("systemJobExecutor")
    private final Executor systemJobExecutor;

    @Transactional(readOnly = true)
    public List<SystemJobDto> getRecentJobs(final Integer limit) {
        final int resolvedLimit = Math.max(1, Math.min(limit == null ? DEFAULT_JOB_LIMIT : limit, 100));
        return systemJobRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, resolvedLimit)).stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public SystemJobDto queueJob(final AppUser requestedByUser, final CreateSystemJobRequest request) {
        final SystemJobType jobType = SystemJobType.fromNullable(request == null ? null : request.jobType());
        if (jobType == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Job type is required.");
        }

        final CreateSystemJobRequest normalizedRequest = request == null
                ? new CreateSystemJobRequest(jobType.name(), null, null, null, null, null)
                : request;
        final SystemJob savedJob = systemJobRepository.save(SystemJob.builder()
                .jobType(jobType)
                .status(SystemJobStatus.QUEUED)
                .requestedBy(resolveRequestedBy(requestedByUser))
                .requestedByUserId(requestedByUser == null ? null : requestedByUser.getId())
                .catalogSource(normalizeCatalogSource(normalizedRequest.catalogSource()))
                .parametersJson(serializeRequest(normalizedRequest))
                .progressMessage("Queued")
                .build());

        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    systemJobExecutor.execute(() -> executeJob(savedJob.getId(), normalizedRequest));
                }
            });
        } else {
            systemJobExecutor.execute(() -> executeJob(savedJob.getId(), normalizedRequest));
        }
        log.info("Queued system job {} ({})", savedJob.getPublicId(), savedJob.getJobType());
        return toDto(savedJob);
    }

    @Transactional
    public void failStaleRunningJobs() {
        final OffsetDateTime staleThreshold = OffsetDateTime.now(ZoneOffset.UTC).minusMinutes(DEFAULT_STALE_JOB_THRESHOLD_MINUTES);
        final List<SystemJob> staleJobs = systemJobRepository.findAllByStatusOrderByCreatedAtAsc(SystemJobStatus.RUNNING).stream()
                .filter(job -> job.getStartedAt() != null && job.getStartedAt().isBefore(staleThreshold))
                .toList();
        if (staleJobs.isEmpty()) {
            return;
        }

        final OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        staleJobs.forEach(job -> {
            job.setStatus(SystemJobStatus.FAILED);
            job.setCompletedAt(now);
            job.setLastHeartbeatAt(now);
            job.setErrorMessage("Marked failed on startup because the previous job process did not complete cleanly.");
            job.setResultSummary("Stale running job was marked failed during startup cleanup.");
        });
        systemJobRepository.saveAll(staleJobs);
        log.warn("Marked {} stale running system jobs as failed during startup cleanup.", staleJobs.size());
    }

    private void executeJob(final Long jobId, final CreateSystemJobRequest request) {
        try {
            markRunning(jobId, "Starting job");
            final SystemJobType jobType = requireJob(jobId).getJobType();
            switch (jobType) {
                case CATALOG_SYNC -> runCatalogSyncJob(jobId, request);
                case CATALOG_CONSOLIDATION -> runCatalogConsolidationJob(jobId);
                case GAME_STATS_REBUILD -> runGameStatsRebuildJob(jobId);
            }
        } catch (Exception exception) {
            log.error("System job {} failed", jobId, exception);
            markFailed(jobId, summarizeException(exception));
        }
    }

    private void runCatalogSyncJob(final Long jobId, final CreateSystemJobRequest request) {
        final CatalogSyncRequest syncRequest = new CatalogSyncRequest(
                normalizeCatalogSource(request.catalogSource()),
                request.pageSize(),
                request.maxBatches(),
                request.maxRecords(),
                request.startOffset()
        );
        updateJobProgress(jobId, "Requesting catalog sync");
        final SystemJob systemJob = requireJob(jobId);
        final CatalogSyncRun syncRun = catalogSyncCoordinator.triggerSync("JOB:" + systemJob.getPublicId(), syncRequest);
        attachSyncRun(jobId, syncRun);

        if (syncRun.getStatus() != CatalogSyncStatus.RUNNING) {
            finalizeFromSyncRun(jobId, syncRun);
            return;
        }

        while (true) {
            try {
                TimeUnit.SECONDS.sleep(2);
            } catch (InterruptedException interruptedException) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Catalog sync job interrupted.");
            }

            final CatalogSyncRun currentRun = catalogSyncRunRepository.findById(syncRun.getId())
                    .orElseThrow(() -> new IllegalStateException("Catalog sync run disappeared while job was active."));
            syncHeartbeat(jobId, currentRun);
            if (currentRun.getStatus() != CatalogSyncStatus.RUNNING) {
                finalizeFromSyncRun(jobId, currentRun);
                return;
            }
        }
    }

    private void runCatalogConsolidationJob(final Long jobId) {
        updateJobProgress(jobId, "Backfilling catalog identity keys");
        catalogConsolidationService.backfillCatalogGroupKeys();
        updateJobProgress(jobId, "Merging duplicate catalog variants");
        catalogConsolidationService.consolidateCatalogVariants();
        markSuccess(jobId, "Catalog consolidation completed.");
    }

    private void runGameStatsRebuildJob(final Long jobId) {
        updateJobProgress(jobId, "Rebuilding game statistics");
        gameStatsMaintenanceService.rebuildAllGameStats();
        markSuccess(jobId, "Game statistics rebuild completed.");
    }

    private void attachSyncRun(final Long jobId, final CatalogSyncRun syncRun) {
        final SystemJob job = requireJob(jobId);
        job.setRelatedSyncRunId(syncRun.getId());
        job.setCatalogSource(syncRun.getCatalogSource() == null ? job.getCatalogSource() : syncRun.getCatalogSource().name());
        job.setProgressMessage(syncRun.getMessage());
        job.setLastHeartbeatAt(OffsetDateTime.now(ZoneOffset.UTC));
        systemJobRepository.save(job);
    }

    private void syncHeartbeat(final Long jobId, final CatalogSyncRun syncRun) {
        final SystemJob job = requireJob(jobId);
        job.setLastHeartbeatAt(OffsetDateTime.now(ZoneOffset.UTC));
        job.setProgressMessage(syncRun.getMessage());
        job.setCatalogSource(syncRun.getCatalogSource() == null ? job.getCatalogSource() : syncRun.getCatalogSource().name());
        systemJobRepository.save(job);
    }

    private void finalizeFromSyncRun(final Long jobId, final CatalogSyncRun syncRun) {
        final SystemJobStatus jobStatus = switch (syncRun.getStatus()) {
            case SUCCESS -> SystemJobStatus.SUCCESS;
            case SKIPPED -> SystemJobStatus.SKIPPED;
            case FAILED -> SystemJobStatus.FAILED;
            case RUNNING -> SystemJobStatus.RUNNING;
        };

        final SystemJob job = requireJob(jobId);
        job.setStatus(jobStatus);
        job.setCompletedAt(OffsetDateTime.now(ZoneOffset.UTC));
        job.setLastHeartbeatAt(OffsetDateTime.now(ZoneOffset.UTC));
        job.setProgressMessage(syncRun.getMessage());
        job.setResultSummary(syncRun.getMessage());
        if (jobStatus == SystemJobStatus.FAILED) {
            job.setErrorMessage(syncRun.getMessage());
        }
        systemJobRepository.save(job);
    }

    private void markRunning(final Long jobId, final String message) {
        final SystemJob job = requireJob(jobId);
        final OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        job.setStatus(SystemJobStatus.RUNNING);
        job.setStartedAt(now);
        job.setLastHeartbeatAt(now);
        job.setProgressMessage(message);
        systemJobRepository.save(job);
    }

    private void updateJobProgress(final Long jobId, final String message) {
        final SystemJob job = requireJob(jobId);
        job.setProgressMessage(message);
        job.setLastHeartbeatAt(OffsetDateTime.now(ZoneOffset.UTC));
        systemJobRepository.save(job);
    }

    private void markSuccess(final Long jobId, final String resultSummary) {
        final SystemJob job = requireJob(jobId);
        final OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        job.setStatus(SystemJobStatus.SUCCESS);
        job.setCompletedAt(now);
        job.setLastHeartbeatAt(now);
        job.setProgressMessage(resultSummary);
        job.setResultSummary(resultSummary);
        systemJobRepository.save(job);
    }

    private void markFailed(final Long jobId, final String errorMessage) {
        final SystemJob job = requireJob(jobId);
        final OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        job.setStatus(SystemJobStatus.FAILED);
        job.setCompletedAt(now);
        job.setLastHeartbeatAt(now);
        job.setErrorMessage(errorMessage);
        job.setProgressMessage(errorMessage);
        job.setResultSummary(errorMessage);
        systemJobRepository.save(job);
    }

    private SystemJob requireJob(final Long jobId) {
        return systemJobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalStateException("System job not found."));
    }

    private String resolveRequestedBy(final AppUser requestedByUser) {
        if (requestedByUser == null) {
            return "SYSTEM";
        }
        return requestedByUser.getUsername() == null || requestedByUser.getUsername().isBlank()
                ? "SYSTEM"
                : requestedByUser.getUsername();
    }

    private String normalizeCatalogSource(final String rawCatalogSource) {
        if (rawCatalogSource == null || rawCatalogSource.isBlank()) {
            return null;
        }
        return rawCatalogSource.trim().toUpperCase();
    }

    private String serializeRequest(final CreateSystemJobRequest request) {
        try {
            return objectMapper.writeValueAsString(request);
        } catch (Exception ignored) {
            return "{}";
        }
    }

    private String summarizeException(final Exception exception) {
        final String message = exception.getMessage();
        if (message == null || message.isBlank()) {
            return exception.getClass().getSimpleName();
        }
        return message;
    }

    private SystemJobDto toDto(final SystemJob job) {
        final CatalogSyncRunDto relatedSyncRun = job.getRelatedSyncRunId() == null
                ? null
                : catalogSyncRunRepository.findById(job.getRelatedSyncRunId()).map(CatalogSyncRunDto::fromEntity).orElse(null);
        return SystemJobDto.fromEntity(job, relatedSyncRun);
    }
}
