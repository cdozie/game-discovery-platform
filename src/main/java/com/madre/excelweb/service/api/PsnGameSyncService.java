package com.madre.excelweb.service.api;

import com.madre.excelweb.jpa.entity.GameData;
import com.madre.excelweb.jpa.repositories.GameRepository;
import com.madre.excelweb.service.IdGenerationService;
import com.madre.excelweb.service.RedissonLockClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@RequiredArgsConstructor
public class PsnGameSyncService {
    private static final String DEFAULT_PSN_SYNC_PATH = "psn-game-sync";
    private static final String PSN_SYNC_PATH = "psn-sync.lock-path";
    private final PsnApiIterator psnApiIterator;
    private final GameRepository gameRepository;
    private final RedissonLockClient redissonLockClient;
    private final Environment env;
    private final IdGenerationService idGenerationService;

    @Scheduled(cron = "0 0 0 * * ?")
    public void syncGames() {
        redissonLockClient.run(env.getProperty(PSN_SYNC_PATH, DEFAULT_PSN_SYNC_PATH), this::syncGamesInternal);
    }

    private void syncGamesInternal() {
        final AtomicInteger batchNum = new AtomicInteger(1);
        final AtomicInteger totalBatchFailures = new AtomicInteger(0);
        // I want to time this and see how long it takes to run
        final long startTime = System.currentTimeMillis();
        while (psnApiIterator.hasNext()) {
            try {
                log.info("Starting sync of batch #{}", batchNum.get());
                final List<GameData> gameData = psnApiIterator.next();
                gameData.parallelStream().forEach(this::gameSyncFilter);
                log.info("Successfully synced batch #{}", batchNum.getAndIncrement());
            } catch (Exception e) {
                log.error("Error syncing batch #{}", batchNum.get(), e);
                totalBatchFailures.incrementAndGet();
                throw e;
            }
        }
        final long totalTime = System.currentTimeMillis() - startTime;

        log.info(completionMessageGenerator(totalBatchFailures.get(), batchNum.get() - 1, totalTime));

    }

    private String completionMessageGenerator(Integer totalBatchFailures, Integer totalBatches, long totalTime) {
        final String successTemplate = "Successfully synced all %s batches of PSN data in %s milliseconds ".formatted(totalBatches, totalTime);
        final String failureTemplate = "Failed to sync %s batches out of %s total batches. Finished in %s milliseconds"
                .formatted(totalBatchFailures, totalBatches, totalTime);

        return totalBatchFailures == 0 ? successTemplate : failureTemplate;
    }

    private void gameSyncFilter(final GameData game) {
        gameRepository.findGameByPsnId(game.getPsnId())
                .ifPresentOrElse(
                        existingGame -> {
                            if (!existingGame.equalsCoreData(game)) {
                                log.info("Updating game: {}", game.getName());

                                game.setId(existingGame.getId());
                                game.setLastModified(OffsetDateTime.now(ZoneOffset.UTC));

                                gameRepository.save(game);

                                log.info("Successfully updated game: {}", game.getName());
                            } else {
                                log.info("Game: {} has not changed, skipping sync", game.getName());
                            }
                        },
                        () -> {
                            log.info("Saving game: {}", game.getName());

                            game.setId(idGenerationService.nextId());
                            game.setCreated(OffsetDateTime.now(ZoneOffset.UTC));
                            game.setLastModified(OffsetDateTime.now(ZoneOffset.UTC));

                            gameRepository.save(game);

                            log.info("Successfully saved game: {}", game.getName());
                        }

                );


    }


}


