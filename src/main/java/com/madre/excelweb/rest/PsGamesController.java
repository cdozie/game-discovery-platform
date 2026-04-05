package com.madre.excelweb.rest;

import com.madre.excelweb.api.model.*;
import com.madre.excelweb.api.model.dto.GameCommentDto;
import com.madre.excelweb.api.model.dto.GameDto;
import com.madre.excelweb.api.model.dto.GameSearchMetadataDto;
import com.madre.excelweb.api.model.dto.GameSearchPageDto;
import com.madre.excelweb.api.model.dto.OffsetPageDto;
import com.madre.excelweb.api.model.dto.WishlistDto;
import com.madre.excelweb.jpa.entity.AppUser;
import com.madre.excelweb.jpa.entity.GameData;
import com.madre.excelweb.jpa.entity.Wishlist;
import com.madre.excelweb.jpa.repositories.WishlistRepository;
import com.madre.excelweb.models.ListGameStatus;
import com.madre.excelweb.service.GameCatalogService;
import com.madre.excelweb.service.GameCommentService;
import com.madre.excelweb.service.GameSearchMetadataService;
import com.madre.excelweb.service.WishlistModificationService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

import static com.madre.excelweb.rest.ControllerUtils.getAppUserFromRequest;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/auth")
public class PsGamesController {
    private final WishlistRepository wishlistRepository;
    private final ControllerUtils controllerUtils;
    private final WishlistModificationService wishlistModificationService;
    private final GameCatalogService gameCatalogService;
    private final GameCommentService gameCommentService;
    private final GameSearchMetadataService gameSearchMetadataService;


    @PostMapping("/wishlist")
    public ResponseEntity<MessageResponse> handleWishlistEntry(HttpServletRequest request, @RequestBody WishlistRequest wishlistRequest) {
        return wishlistModificationService.upsertList(request, wishlistRequest);
    }

    @DeleteMapping("/wishlist/{gameId}")
    public ResponseEntity<MessageResponse> removeFromWishlist(HttpServletRequest request,
                                                              @PathVariable String gameId) {
        return wishlistModificationService.removeFromWishlist(request, gameId);
    }

    @PostMapping("/games/{gameId}/rating")
    public ResponseEntity<MessageResponse> rateGame(HttpServletRequest request, @PathVariable String gameId, @RequestBody RatingRequest ratingRequest) {
        return wishlistModificationService.updateRating(request, gameId, ratingRequest);
    }


    @PutMapping("/games/{gameId}/status/{status}")
    public ResponseEntity<MessageResponse> updateStatus(HttpServletRequest request, @PathVariable String gameId, @PathVariable String status) {
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
    public ResponseEntity<?> getWishlistEntry(HttpServletRequest request, @PathVariable String gameId) {
        try {

            log.info("Attempting to get wishlist entry");

            final AppUser appUser = getAppUserFromRequest(request);
            final GameData gameData = gameCatalogService.getGameEntity(gameId).orElse(null);
            if (gameData == null) {
                log.info("Game with id {} not found", gameId);
                return ResponseEntity.badRequest().body(new MessageResponse(MessageType.ERROR, "Game not found"));
            }

            log.info("Found user: {}, getting wishlist entry for game with id: {}", appUser.getUsername(), gameId);

            final WishlistDto wishlistDto = wishlistRepository.findByUserinfoIdAndGameinfoId(appUser.getId(), gameData.getId())
                    .map(controllerUtils::wishlistEntryToWishlistDto).orElse(null);

            if (wishlistDto == null) {
                log.info("Wishlist entry not found. Creating empty wishlist entry for game with id: {}", gameId);

                final WishlistDto newWishlistEntry = controllerUtils.wishlistEntryToWishlistDto(Wishlist.builder()
                        .userinfo(appUser)
                        .gameinfo(gameData)
                        .selectedPlatform(null)
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
    public ResponseEntity<GameDto> retrieveGame(HttpServletRequest request, @PathVariable String gameId) {
        log.info("Attempting to get game with id {}", gameId);
        try {
            final AppUser user = getAppUserFromRequest(request);

            if (user == null) {
                return ResponseEntity.badRequest().body(null);
            }
            final Optional<GameDto> gameDto = gameCatalogService.getGame(user, gameId);
            if (gameDto.isEmpty()) {
                return ResponseEntity.badRequest().body(null);
            }

            log.info("Successfully retrieved game with id : {}.", gameId);
            return ResponseEntity.ok().body(gameDto.get());

        } catch (Exception e) {
            log.error("Error getting game, please reload and try again", e);
            return ResponseEntity.badRequest().build();
        }

    }

    @GetMapping("/games/search")
    public ResponseEntity<?> gameSearchResults(HttpServletRequest request,
                                               @RequestParam String query,
                                               @RequestParam Integer limit,
                                               @RequestParam(required = false) String platform,
                                               @RequestParam(required = false) String category) {
        try {
            log.info("Attempting to search for games with query {}", query);

            AppUser appUser = getAppUserFromRequest(request);

            if (appUser == null) {
                return ResponseEntity.badRequest().body(new MessageResponse(MessageType.ERROR, "User not found"));
            }

            final List<GameDto> gameDtos = gameCatalogService.searchGames(appUser, query, limit, platform, category);

            log.info("Successfully retrieved games with query : {}.", query);
            return ResponseEntity.ok().body(gameDtos);

        } catch (Exception e) {
            log.error("Error getting games, please reload and try again", e);
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/games/search-page")
    public ResponseEntity<GameSearchPageDto> gameSearchPage(HttpServletRequest request,
                                                            @RequestParam(required = false, defaultValue = "") String query,
                                                            @RequestParam(required = false) Integer limit,
                                                            @RequestParam(required = false) Integer offset,
                                                            @RequestParam(required = false) String platform,
                                                            @RequestParam(required = false) String category,
                                                            @RequestParam(required = false) String publisher,
                                                            @RequestParam(required = false) Boolean publisherExact,
                                                            @RequestParam(required = false) String catalogSource,
                                                            @RequestParam(required = false) Boolean hasDescription,
                                                            @RequestParam(required = false) Boolean hasImage,
                                                            @RequestParam(required = false) String sort) {
        final AppUser appUser = getAppUserFromRequest(request);
        return ResponseEntity.ok(gameCatalogService.searchGamesPage(appUser, query, limit, offset, platform, category, publisher, publisherExact, catalogSource, hasDescription, hasImage, sort));
    }

    @GetMapping("/games/search-metadata")
    public ResponseEntity<GameSearchMetadataDto> gameSearchMetadata(@RequestParam(required = false) final String publisherQuery,
                                                                    @RequestParam(required = false) final String platformQuery,
                                                                    @RequestParam(required = false) final String categoryQuery,
                                                                    @RequestParam(required = false) final Integer limit) {
        return ResponseEntity.ok(gameSearchMetadataService.getMetadata(publisherQuery, platformQuery, categoryQuery, limit));
    }

    @GetMapping("/games/{gameId}/comments")
    public ResponseEntity<OffsetPageDto<GameCommentDto>> getGameComments(HttpServletRequest request,
                                                                         @PathVariable String gameId,
                                                                         @RequestParam(required = false) Integer limit,
                                                                         @RequestParam(required = false) Integer offset) {
        return ResponseEntity.ok(gameCommentService.getComments(getAppUserFromRequest(request), gameId, limit, offset));
    }

    @PostMapping("/games/{gameId}/comments")
    public ResponseEntity<GameCommentDto> createGameComment(HttpServletRequest request,
                                                            @PathVariable String gameId,
                                                            @RequestBody CreateGameCommentRequest createGameCommentRequest) {
        final AppUser appUser = getAppUserFromRequest(request);
        return ResponseEntity.ok(gameCommentService.createComment(appUser, gameId, createGameCommentRequest));
    }

    @PutMapping("/games/{gameId}/comments/{commentId}")
    public ResponseEntity<GameCommentDto> updateGameComment(HttpServletRequest request,
                                                            @PathVariable String gameId,
                                                            @PathVariable String commentId,
                                                            @RequestBody UpdateGameCommentRequest updateGameCommentRequest) {
        final AppUser appUser = getAppUserFromRequest(request);
        return ResponseEntity.ok(gameCommentService.updateComment(appUser, gameId, commentId, updateGameCommentRequest));
    }

    @DeleteMapping("/games/{gameId}/comments/{commentId}")
    public ResponseEntity<MessageResponse> deleteGameComment(HttpServletRequest request,
                                                             @PathVariable String gameId,
                                                             @PathVariable String commentId) {
        final AppUser appUser = getAppUserFromRequest(request);
        gameCommentService.deleteComment(appUser, gameId, commentId);
        return ResponseEntity.ok(new MessageResponse(MessageType.SUCCESS, "Comment removed."));
    }

    @GetMapping("/games/random-id")
    public ResponseEntity<IdResponse> retrieveRandomGameId() {
        try {
            log.info("Attempting to get random game id");
            final Optional<String> gameId = gameCatalogService.getRandomGameId();
            if (gameId.isEmpty()) {
                log.info("No games found");
                return ResponseEntity.notFound().build();
            }
            log.info("Successfully retrieved random game id: {}.", gameId.get());
            return ResponseEntity.ok().body(new IdResponse(gameId.get()));

        } catch (Exception e) {
            log.error("Error getting random game id", e);
            return ResponseEntity.badRequest().build();
        }
    }

}
