package com.madre.excelweb.service.redis;

import com.madre.excelweb.jpa.entity.GameData;
import com.madre.excelweb.jpa.repositories.GameRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RedissonClient;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
public class GameStatsService {
    private final RedissonClient redissonClient;
    private final GameRepository gameRepository;

    public void recordGameInteractions(Long gameId) {
        redissonClient.getAtomicLong(GameStatsRedis.INTERACTIONS.getName() + gameId).incrementAndGet();

    }

    @Scheduled(fixedDelayString = "${spring.data.redis.migration}")
    public void migrateGameStats() {

        redissonClient.getKeys().getKeysByPattern("game:*").forEach(key -> {

            final String[] split = key.split(":");
            final Long gameId = Long.valueOf(split[2]);
            final Long value = redissonClient.getAtomicLong(key).get();

            if (value > 0) {
                Optional<GameData> gameStatsOptional = gameRepository.findById(gameId);

                if (gameStatsOptional.isEmpty()) {
                    log.error("Game stats not found for game id: {}", gameId);
                    return;
                }

                log.info("Logging {} interactions for game id: {}", value, gameId);
                final GameData gameData = gameStatsOptional.get();
                gameData.setInteractions(gameData.getInteractions() + value);
                gameRepository.save(gameData);
                log.info("Successfully logged interactions for game id: {}", gameId);
                // Delete the key
                redissonClient.getKeys().delete(key);

            } else {
                log.info("No stats to migrate for game id: {}", gameId);
            }


        });
    }
}
