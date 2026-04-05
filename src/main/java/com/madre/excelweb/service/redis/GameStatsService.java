package com.madre.excelweb.service.redis;

import com.madre.excelweb.jpa.entity.GameData;
import com.madre.excelweb.jpa.repositories.GameRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.scheduling.annotation.Scheduled;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

@Slf4j
@RequiredArgsConstructor
public class GameStatsService {
    private static final String INTERACTION_KEY_PATTERN = GameStatsRedis.INTERACTIONS.getName() + "*";
    private final StringRedisTemplate stringRedisTemplate;
    private final GameRepository gameRepository;

    public void recordGameInteractions(Long gameId) {
        stringRedisTemplate.opsForValue().increment(GameStatsRedis.INTERACTIONS.getName() + gameId);
    }

    @Scheduled(fixedDelayString = "${spring.data.redis.migration}")
    public void migrateGameStats() {
        for (String key : scanKeys(INTERACTION_KEY_PATTERN)) {
            final String[] split = key.split(":");
            final Long gameId = Long.valueOf(split[2]);
            final Long value = Optional.ofNullable(stringRedisTemplate.opsForValue().get(key))
                    .map(Long::valueOf)
                    .orElse(0L);

            if (value <= 0) {
                log.info("No stats to migrate for game id: {}", gameId);
                continue;
            }

            final Optional<GameData> gameStatsOptional = gameRepository.findById(gameId);
            if (gameStatsOptional.isEmpty()) {
                log.error("Game stats not found for game id: {}", gameId);
                continue;
            }

            log.info("Logging {} interactions for game id: {}", value, gameId);
            final GameData gameData = gameStatsOptional.get();
            final Long interactions = gameData.getInteractions() == null ? 0L : gameData.getInteractions();
            gameData.setInteractions(interactions + value);
            gameRepository.save(gameData);
            stringRedisTemplate.delete(key);
        }
    }

    private Set<String> scanKeys(String pattern) {
        return Optional.ofNullable(stringRedisTemplate.execute((RedisConnection connection) -> {
            final Set<String> keys = new LinkedHashSet<>();
            try (var cursor = connection.scan(ScanOptions.scanOptions().match(pattern).count(1000).build())) {
                while (cursor.hasNext()) {
                    keys.add(new String(cursor.next(), StandardCharsets.UTF_8));
                }
            }
            return keys;
        })).orElseGet(Set::of);
    }
}
