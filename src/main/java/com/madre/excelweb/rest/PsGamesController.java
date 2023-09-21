package com.madre.excelweb.rest;

import com.madre.excelweb.api.model.*;
import com.madre.excelweb.api.model.dto.GameDto;
import com.madre.excelweb.api.model.dto.WishlistDto;
import com.madre.excelweb.jpa.entity.AppUser;
import com.madre.excelweb.jpa.entity.GameData;
import com.madre.excelweb.jpa.entity.Wishlist;
import com.madre.excelweb.jpa.repositories.GameRepository;
import com.madre.excelweb.jpa.repositories.WishlistRepository;
import com.madre.excelweb.models.ListGameStatus;
import com.madre.excelweb.service.WishlistModificationService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

import static com.madre.excelweb.rest.ControllerUtils.getAppUserFromRequest;
import static com.madre.excelweb.rest.ControllerUtils.validateGame;

@Slf4j
@CrossOrigin(value = "*", maxAge = 3600)
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/auth")
public class PsGamesController {
    private final GameRepository gameRepository;
    private final WishlistRepository wishlistRepository;
    private final ControllerUtils controllerUtils;
    private final WishlistModificationService wishlistModificationService;


    @PostMapping("/wishlist")
    public ResponseEntity<MessageResponse> handleWishlistEntry(HttpServletRequest request, @RequestBody WishlistRequest wishlistRequest) {
        return wishlistModificationService.upsertList(request, wishlistRequest);
    }

    @DeleteMapping("/wishlist/{gameId}")
    public ResponseEntity<MessageResponse> removeFromWishlist(HttpServletRequest request,
                                                              @PathVariable Long gameId) {
        return wishlistModificationService.removeFromWishlist(request, gameId);
    }

    @PostMapping("/games/{gameId}/rating")
    public ResponseEntity<MessageResponse> rateGame(HttpServletRequest request, @PathVariable Long gameId, @RequestBody RatingRequest ratingRequest) {
        return wishlistModificationService.updateRating(request, gameId, ratingRequest);
    }


    @PutMapping("/games/{gameId}/status/{status}")
    public ResponseEntity<MessageResponse> updateStatus(HttpServletRequest request, @PathVariable Long gameId, @PathVariable String status) {
        return wishlistModificationService.updateStatus(request, gameId, status);
    }


    @GetMapping("/games/wishlist")
    public ResponseEntity<?> getWishlist(HttpServletRequest request) {
        try {

            log.info("Attempting to get wishlist");

            final AppUser appUser = getAppUserFromRequest(request);
            log.info("Found user: {}, getting wishlist", appUser.getUsername());

            final List<WishlistDto> wishlist =
                    wishlistRepository.findByUserinfoId(appUser.getId()).stream()
                            .map(controllerUtils::wishlistEntryToWishlistDto).toList();

            return ResponseEntity.ok().body(wishlist);

        } catch (Exception e) {
            log.error("Error getting wishlist", e);
            return ResponseEntity.badRequest().body(new MessageResponse(MessageType.ERROR, "Error getting wishlist. Please try again later"));
        }
    }

    @GetMapping("/games/wishlist/{gameId}")
    public ResponseEntity<?> getWishlistEntry(HttpServletRequest request, @PathVariable Long gameId) {
        try {

            log.info("Attempting to get wishlist entry");

            final AppUser appUser = getAppUserFromRequest(request);
            log.info("Found user: {}, getting wishlist entry for game with id: {}", appUser.getUsername(), gameId);

            final WishlistDto wishlistDto = wishlistRepository.findByUserinfoIdAndGameinfoId(appUser.getId(), gameId)
                    .map(controllerUtils::wishlistEntryToWishlistDto).orElse(null);

            if (wishlistDto == null) {
                log.info("Wishlist entry not found. Creating empty wishlist entry for game with id: {}", gameId);

                final GameData gameData = gameRepository.findGameById(gameId).orElse(null);
                if (gameData == null) {
                    log.info("Game with id {} not found", gameId);
                    return ResponseEntity.badRequest().body(new MessageResponse(MessageType.ERROR, "Game not found"));
                }

                final WishlistDto newWishlistEntry = controllerUtils.wishlistEntryToWishlistDto(Wishlist.builder()
                        .userinfo(appUser)
                        .gameinfo(gameData)
                        .status(ListGameStatus.PLAN_TO_PLAY)
                        .build());

                return ResponseEntity.ok().body(newWishlistEntry);
            }

            return ResponseEntity.ok().body(wishlistDto);
        } catch (Exception e) {
            log.error("Error getting wishlist entry for game with id {}", gameId, e);
            return ResponseEntity.badRequest().body(new MessageResponse(MessageType.ERROR, "Error getting wishlist entry. Please try again later"));
        }
    }

    @GetMapping("/games/{gameId}")
    public ResponseEntity<GameDto> retrieveGame(HttpServletRequest request, @PathVariable Long gameId) {
        log.info("Attempting to get game with id {}", gameId);
        try {
            final AppUser user = getAppUserFromRequest(request);

            if (user == null) {
                return ResponseEntity.badRequest().body(null);
            }
            Optional<GameData> game = gameRepository.findGameById(gameId);

            if (validateGame(game, gameId) != null) {
                return ResponseEntity.badRequest().body(null);
            }

            final GameData gameData = game.get();

            final GameDto gameDto = controllerUtils.gameDataToGameDto(user, gameData);


            log.info("Successfully retrieved game with id : {}.", gameId);
            return ResponseEntity.ok().body(gameDto);

        } catch (Exception e) {
            log.error("Error getting game, please reload and try again", e);
            return ResponseEntity.badRequest().build();
        }

    }

    @GetMapping("/games/search")
    public ResponseEntity<?> gameSearchResults(HttpServletRequest request, @RequestParam String query, @RequestParam Integer limit) {
        try {
            log.info("Attempting to search for games with query {}", query);

            AppUser appUser = getAppUserFromRequest(request);

            if (appUser == null) {
                return ResponseEntity.badRequest().body(new MessageResponse(MessageType.ERROR, "User not found"));
            }

            final List<GameData> games = gameRepository.searchByQuery(query.toLowerCase(), limit);

            final List<GameDto> gameDtos = games.stream().map(gameData ->
                    controllerUtils.gameDataToGameDto(appUser, gameData)
            ).toList();

            log.info("Successfully retrieved games with query : {}.", query);
            return ResponseEntity.ok().body(gameDtos);

        } catch (Exception e) {
            log.error("Error getting games, please reload and try again", e);
            return ResponseEntity.badRequest().build();
        }
    }

    protected Boolean gameInWishlist(final AppUser appUser, final GameData gameData) {
        final Wishlist existingWishlistEntry = wishlistRepository.findByUserinfoIdAndGameinfoId(appUser.getId(), gameData.getId()).orElse(null);
        return existingWishlistEntry != null;
    }

    @GetMapping("/games/random-id")
    public ResponseEntity<IdResponse> retrieveRandomGameId() {
        try {
            log.info("Attempting to get random game id");
            Optional<Long> gameId = gameRepository.findRandomGameId();

            if (gameId.isEmpty()) {
                log.info("No games found");
                return ResponseEntity.notFound().build();
            }

            log.info("Successfully retrieved random game id: {}.", gameId.get());
            return ResponseEntity.ok().body(new IdResponse(gameId.get().toString()));

        } catch (Exception e) {
            log.error("Error getting random game id", e);
            return ResponseEntity.badRequest().build();
        }
    }

}
