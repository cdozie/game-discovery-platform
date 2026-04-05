package com.madre.excelweb.service;

import com.madre.excelweb.api.model.MessageResponse;
import com.madre.excelweb.api.model.MessageType;
import com.madre.excelweb.api.model.RatingRequest;
import com.madre.excelweb.api.model.WishlistRequest;
import com.madre.excelweb.jpa.entity.AppUser;
import com.madre.excelweb.jpa.entity.GameData;
import com.madre.excelweb.jpa.entity.Wishlist;
import com.madre.excelweb.jpa.repositories.GameRepository;
import com.madre.excelweb.jpa.repositories.WishlistRepository;
import com.madre.excelweb.models.ListGameStatus;
import com.madre.excelweb.service.util.CatalogGroupingUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

import java.util.Objects;
import java.util.Optional;

import static com.madre.excelweb.rest.ControllerUtils.getAppUserFromRequest;

@Slf4j
@RequiredArgsConstructor
public class WishlistModificationService {
    private final WishlistRepository wishlistRepository;
    private final GameRepository gameRepository;
    private final PublicEntityLookupService publicEntityLookupService;

    public ResponseEntity<MessageResponse> upsertList(HttpServletRequest request, WishlistRequest wishlistRequest) {
        try {
            log.info("Attempting to add game to wishlist");
            final String requestedGameId = wishlistRequest.getGameId();
            GameData gameData = publicEntityLookupService.findGame(requestedGameId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Game not found"));

            final AppUser appUser = getAppUserFromRequest(request);

            final Optional<Wishlist> existingWishlistEntryOptional = wishlistRepository
                    .findByUserinfoIdAndGameinfoId(appUser.getId(), gameData.getId());

            if (existingWishlistEntryOptional.isPresent()) {
                log.info("Updating Game: {} in wishlist of user: {}", gameData.getName(), appUser.getUsername());
                return updateWishlistEntry(appUser, gameData, wishlistRequest, existingWishlistEntryOptional.get());
            } else {
                log.info("Adding Game: {} to wishlist of user: {}", gameData.getName(), appUser.getUsername());
                return addNewWishlistEntry(appUser, gameData, wishlistRequest);
            }

        } catch (ResponseStatusException ex) {
            log.warn(ex.getMessage());
            return ResponseEntity.badRequest().body(new MessageResponse(MessageType.ERROR, ex.getReason()));
        } catch (Exception e) {
            log.error("Error adding/modifying game of id : {}", wishlistRequest.getGameId(), e);
            return ResponseEntity.badRequest().body(new MessageResponse(MessageType.ERROR, "Error with wishlist. Please try again later"));
        }
    }

    public ResponseEntity<MessageResponse> removeFromWishlist(HttpServletRequest request, @NonNull final String gameId) {
        try {
            log.info("Removing game with id: {} from wishlist", gameId);
            final AppUser appUser = getAppUserFromRequest(request);
            final Optional<GameData> gameOptional = publicEntityLookupService.findGame(gameId);
            if (gameOptional.isEmpty()) {
                return ResponseEntity.badRequest().body(new MessageResponse(MessageType.ERROR, "Game not found"));
            }
            final GameData gameData = gameOptional.get();
            log.info("Found user: {} and game: {}, removing from wishlist", appUser.getUsername(), gameData.getName());

            final Optional<Wishlist> existingWishlistEntryOptional = wishlistRepository
                    .findByUserinfoIdAndGameinfoId(appUser.getId(), gameData.getId());

            if (existingWishlistEntryOptional.isEmpty()) {
                log.info("Game: {} not in wishlist of user: {}", gameData.getName(), appUser.getUsername());
                return ResponseEntity.badRequest().body(new MessageResponse(MessageType.ERROR, "Game not in wishlist"));
            }

            final Wishlist existingWishlistEntry = existingWishlistEntryOptional.get();
            wishlistRepository.delete(existingWishlistEntry);
            refreshGameStats(gameData.getId());
            log.info("Successfully removed game: {} from wishlist of user: {}", gameData.getName(), appUser.getUsername());

        } catch (Exception e) {
            log.error("Error removing game of id : {} from wishlist", gameId, e);
            return ResponseEntity.badRequest().body(new MessageResponse(MessageType.ERROR,
                    "Error occurred with wishlist. Please try again later"));
        }
        return ResponseEntity.ok().body(new MessageResponse(MessageType.SUCCESS, "Game successfully removed from wishlist"));
    }

    private ResponseEntity<MessageResponse> addNewWishlistEntry(final AppUser appUser, final GameData gameData, final WishlistRequest wishlistRequest) {
        final Wishlist wishlistEntry = Wishlist.builder()
                .gameinfo(gameData)
                .userinfo(appUser)
                .rating(normalizeRating(wishlistRequest.getRating()))
                .selectedPlatform(resolveSelectedPlatform(wishlistRequest, gameData, null))
                .status(ListGameStatus.fromStringSwitch(wishlistRequest.getStatus()))
                .build();

        wishlistRepository.save(wishlistEntry);
        refreshGameStats(gameData.getId());

        log.info("Successfully added game: {} to the wishlist of user: {}", gameData.getName(), appUser.getUsername());
        return ResponseEntity.ok().body(new MessageResponse(MessageType.SUCCESS, "Game successfully added to wishlist"));
    }


    private ResponseEntity<MessageResponse> updateWishlistEntry(final AppUser appUser, final GameData gameData,
                                                                final WishlistRequest wishlistRequest, final Wishlist existingWishlistEntry) {
        log.info("Game: {} already in wishlist of user: {}. Seeking to update wishlist entry", gameData.getName(), appUser.getUsername());

        boolean isChanged = false;
        final String resolvedSelectedPlatform = resolveSelectedPlatform(wishlistRequest, gameData, existingWishlistEntry.getSelectedPlatform());
        final Double normalizedRating = normalizeRating(wishlistRequest.getRating());

        if (wishlistRequest.getRating() == null) {
            log.info("Removing rating for game: {} in wishlist of user: {}", gameData.getName(), appUser.getUsername());
            existingWishlistEntry.setRating(null);
            isChanged = true;
        }

        if (wishlistRequest.getRating() != null && !Objects.equals(normalizedRating, existingWishlistEntry.getRating())) {
            log.info("Updating rating of game: {} in wishlist of user: {}", gameData.getName(), appUser.getUsername());
            existingWishlistEntry.setRating(normalizedRating);
            isChanged = true;
        }


        if (wishlistRequest.getStatus() != null && ListGameStatus.fromStringSwitch(wishlistRequest.getStatus()) != existingWishlistEntry.getStatus()) {
            log.info("Updating status of game: {} in wishlist of user: {}", gameData.getName(), appUser.getUsername());
            existingWishlistEntry.setStatus(ListGameStatus.fromStringSwitch(wishlistRequest.getStatus()));
            isChanged = true;
        }

        if (!Objects.equals(existingWishlistEntry.getSelectedPlatform(), resolvedSelectedPlatform)) {
            log.info("Updating selected platform of game: {} in wishlist of user: {}", gameData.getName(), appUser.getUsername());
            existingWishlistEntry.setSelectedPlatform(resolvedSelectedPlatform);
            isChanged = true;
        }

        if (isChanged) {
            log.info("Updated Wishlist Entry for game with id: {}  for user: {}",
                    gameData.getId(), appUser.getUsername());
            wishlistRepository.save(existingWishlistEntry);
            refreshGameStats(gameData.getId());
        } else {
            log.info("No changes to Wishlist Entry for game with id: {} for user: {} so no updating",
                    gameData.getId(), appUser.getUsername());
        }

        return ResponseEntity.ok().body(new MessageResponse(MessageType.SUCCESS, "Game successfully updated in wishlist"));
    }

    public ResponseEntity<MessageResponse> updateRating(HttpServletRequest request, String gameId, RatingRequest ratingRequest) {
        try {
            log.info("Rating game with id {} in wishlist", gameId);
            final GameData gameData = publicEntityLookupService.findGame(gameId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Game not found"));

            final AppUser appUser = getAppUserFromRequest(request);

            log.info("Found user: {} and game: {}. Finding wishlist entry", appUser.getUsername(), gameData.getName());

            final Optional<Wishlist> existingWishlistEntryOptional = wishlistRepository
                    .findByUserinfoIdAndGameinfoId(appUser.getId(), gameData.getId());

            if (existingWishlistEntryOptional.isEmpty()) {
                log.info("Game: {} not in wishlist of user: {}. Cannot change rating", gameData.getName(), appUser.getUsername());
                return ResponseEntity.badRequest().body(new MessageResponse(MessageType.ERROR, "Game not in wishlist"));
            }

            final Wishlist existingWishlistEntry = existingWishlistEntryOptional.get();
            final Double normalizedRating = normalizeRating(ratingRequest.getRating());
            log.info(String.valueOf(normalizedRating));
            log.info("Found user: {} and game: {}. Changing rating", appUser.getUsername(), gameData.getName());
            existingWishlistEntry.setRating(normalizedRating);
            wishlistRepository.save(existingWishlistEntry);
            refreshGameStats(gameData.getId());

        } catch (ResponseStatusException ex) {
            log.warn(ex.getMessage());
            return ResponseEntity.badRequest().body(new MessageResponse(MessageType.ERROR, ex.getReason()));

        } catch (Exception e) {
            log.error("Error adding/modifying rating of game with id : {}", gameId, e);
            return ResponseEntity.badRequest().body(new MessageResponse(MessageType.ERROR,
                    "Error occurred with wishlist. Please try again later"));
        }

        return ResponseEntity.ok().body(new MessageResponse(MessageType.SUCCESS,
                "Game rating successfully updated in wishlist"));

    }

    private Double normalizeRating(final Double rawRating) {
        if (rawRating == null) {
            return null;
        }

        double normalizedRating = rawRating;
        if (normalizedRating > 10.0 && normalizedRating <= 100.0) {
            normalizedRating = normalizedRating / 10.0;
        }

        if (normalizedRating < 0.0 || normalizedRating > 10.0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Rating must be between 0 and 10.");
        }

        return Math.round(normalizedRating * 10.0) / 10.0;
    }

    public ResponseEntity<MessageResponse> updateStatus(HttpServletRequest request, String gameId, String status) {
        try {
            log.info("Updating game status");
            final GameData gameData = publicEntityLookupService.findGame(gameId).orElseThrow(() ->
                    new ResponseStatusException(HttpStatus.BAD_REQUEST, "Game not found"));
            final AppUser appUser = getAppUserFromRequest(request);

            log.info("Found user: {} and game: {}. Updating status", appUser.getUsername(), gameData.getName());

            final Optional<Wishlist> existingWishlistEntryOptional = wishlistRepository
                    .findByUserinfoIdAndGameinfoId(appUser.getId(), gameData.getId());

            if (existingWishlistEntryOptional.isEmpty()) {
                log.info("Game: {} not in wishlist of user: {}. Cannot change status", gameData.getName(), appUser.getUsername());
                return ResponseEntity.badRequest().body(new MessageResponse(MessageType.ERROR, "Game not in wishlist"));
            }

            final Wishlist existingWishlistEntry = existingWishlistEntryOptional.get();

            log.info("Found user: {} and game: {}. Changing status", appUser.getUsername(), gameData.getName());
            existingWishlistEntry.setStatus(ListGameStatus.fromStringSwitch(status));
            wishlistRepository.save(existingWishlistEntry);
            refreshGameStats(gameData.getId());
            log.info("Successfully updated status of game: {} from the wishlist of user: {}",
                    gameData.getName(), appUser.getUsername());
        } catch (ResponseStatusException ex) {
            log.warn(ex.getMessage());
            return ResponseEntity.badRequest().body(new MessageResponse(MessageType.ERROR, ex.getReason()));
        } catch (Exception e) {
            log.error("Error adding/modifying game of id : {}", gameId, e);
            return ResponseEntity.badRequest().body(new MessageResponse(MessageType.ERROR,
                    "Error occurred with wishlist. Please try again later"));
        }

        return ResponseEntity.ok().body(new MessageResponse(MessageType.SUCCESS,
                "Game status successfully updated in wishlist"));
    }

    private void refreshGameStats(final Long gameId) {
        gameRepository.findGameById(gameId).ifPresent(gameData -> {
            final var wishlistEntries = wishlistRepository.findByGameinfoId(gameId);
            final var averageRating = wishlistEntries.stream()
                    .map(Wishlist::getRating)
                    .filter(Objects::nonNull)
                    .mapToDouble(Double::doubleValue)
                    .average();

            gameData.setWishlistTotal((long) wishlistEntries.size());
            gameData.setAverageRating(averageRating.isPresent()
                    ? Math.round(averageRating.getAsDouble() * 10.0) / 10.0
                    : null);
            gameRepository.save(gameData);
        });
    }

    private String resolveSelectedPlatform(final WishlistRequest wishlistRequest,
                                           final GameData gameData,
                                           final String fallbackSelectedPlatform) {
        final var platformOptions = CatalogGroupingUtils.parseDelimitedValues(gameData.getPlatforms());
        if (platformOptions.isEmpty()) {
            return null;
        }

        if (platformOptions.size() == 1) {
            return platformOptions.get(0);
        }

        final String requestedPlatform = wishlistRequest == null ? null : wishlistRequest.getSelectedPlatform();
        final String selectedPlatformCandidate = CatalogGroupingUtils.canonicalizePlatformValue((requestedPlatform == null || requestedPlatform.isBlank())
                ? fallbackSelectedPlatform
                : requestedPlatform.trim());

        if (selectedPlatformCandidate == null || selectedPlatformCandidate.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Please choose which platform you want to track for this game.");
        }

        return platformOptions.stream()
                .filter(platform -> platform.equalsIgnoreCase(selectedPlatformCandidate))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selected platform is not valid for this game."));
    }

}
