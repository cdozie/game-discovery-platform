package com.madre.excelweb.rest;

import com.madre.excelweb.api.model.GameIdRequest;
import com.madre.excelweb.api.model.MessageResponse;
import com.madre.excelweb.api.model.MessageType;
import com.madre.excelweb.api.model.dto.GameDataInteractionsDto;
import com.madre.excelweb.jpa.entity.AppUser;
import com.madre.excelweb.jpa.entity.GameData;
import com.madre.excelweb.jpa.repositories.GameRepository;
import com.madre.excelweb.service.redis.GameStatsService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@CrossOrigin(value = "*", maxAge = 3600)
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/auth")
public class StatsController {
    private final GameRepository gameRepository;
    private final GameStatsService gameStatsService;
    private final ControllerUtils controllerUtils;

    @GetMapping("/games/most-interacted-with")
    public ResponseEntity<?> retrieveMostInteractedGames(HttpServletRequest request, @RequestParam Long limit,
                                                         @RequestParam(required = false) Long lastId,
                                                         @RequestParam(required = false) Long lastInteractions) {
        try {
            log.info("Attempting to get most interacted games");
            log.info("Limit: {}", limit);
            log.info("Last Id: {}", lastId);
            log.info("Last Interactions: {}", lastInteractions);


            final AppUser appUser = ControllerUtils.getAppUserFromRequest(request);


            final List<GameData> games = gameRepository
                    .findAllByOrderByInteractionsDesc(limit, lastId, lastInteractions);

            final List<GameDataInteractionsDto> gameDtos = games.stream().map(gameStats ->
                    GameDataInteractionsDto.builder()
                            .id(gameStats.getId().toString())
                            .name(gameStats.getName())
                            .psnId(gameStats.getPsnId())
                            .url(gameStats.getUrl())
                            .price(gameStats.getPrice())
                            .releaseDate(gameStats.getReleaseDate())
                            .description(gameStats.getDescription())
                            .categories(gameStats.getCategories())
                            .platforms(gameStats.getPlatforms())
                            .provider(gameStats.getProviderName())
                            .imageUrl(gameStats.getImageUrl())
                            .interactions(gameStats.getInteractions().toString())
                            .inWishlist(controllerUtils.gameInWishlist(appUser, gameStats))
                            .build()).toList();

            log.info("Successfully retrieved most interacted games.");
            return ResponseEntity.ok().body(gameDtos);

        } catch (Exception e) {
            log.error("Error getting most interacted games, please reload and try again", e);
            return ResponseEntity.badRequest().body(new MessageResponse(MessageType.ERROR,
                    "Error getting most interacted with games. Please try again later"));
        }
    }

    @PostMapping("/games/interactions")
    public void recordGameInteraction(@RequestBody GameIdRequest gameIdRequest) {
        Long gameId = gameIdRequest.getGameId();
        log.info("Attempting to record game interaction for game id: {}", gameId);
        try {
            gameStatsService.recordGameInteractions(gameId);
            log.info("Successfully recorded game interaction for game id: {}", gameId);
        } catch (Exception e) {
            log.error("Error recording game interaction for game id: {}", gameId, e);
        }
    }


}
