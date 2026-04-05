package com.madre.excelweb.rest;

import com.madre.excelweb.api.model.GameIdRequest;
import com.madre.excelweb.api.model.MessageResponse;
import com.madre.excelweb.api.model.MessageType;
import com.madre.excelweb.api.model.dto.GameDataInteractionsDto;
import com.madre.excelweb.jpa.entity.AppUser;
import com.madre.excelweb.service.GameCatalogService;
import com.madre.excelweb.service.redis.GameStatsService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/auth")
public class StatsController {
    private final GameCatalogService gameCatalogService;
    private final GameStatsService gameStatsService;
    private final com.madre.excelweb.service.PublicEntityLookupService publicEntityLookupService;

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


            final List<GameDataInteractionsDto> gameDtos = gameCatalogService
                    .getPopularGames(appUser, limit, lastId, lastInteractions);

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
        final String gameId = gameIdRequest.getGameId();
        log.info("Attempting to record game interaction for game id: {}", gameId);
        try {
            gameStatsService.recordGameInteractions(publicEntityLookupService.requireGame(gameId).getId());
            log.info("Successfully recorded game interaction for game id: {}", gameId);
        } catch (Exception e) {
            log.error("Error recording game interaction for game id: {}", gameId, e);
        }
    }


}
