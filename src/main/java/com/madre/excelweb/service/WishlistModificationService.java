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
import static com.madre.excelweb.rest.ControllerUtils.validateGame;

@Slf4j
@RequiredArgsConstructor
public class WishlistModificationService {
    private final WishlistRepository wishlistRepository;
    private final GameRepository gameRepository;

    public ResponseEntity<MessageResponse> upsertList(HttpServletRequest request, WishlistRequest wishlistRequest) {
        try {
            log.info("Attempting to add game to wishlist");
            GameData gameData = gameRepository.findGameById(wishlistRequest.getGameId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Game not found"));

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

    public ResponseEntity<MessageResponse> removeFromWishlist(HttpServletRequest request, @NonNull final Long gameId) {
        try {
            log.info("Removing game with id: {} from wishlist", gameId);
            final AppUser appUser = getAppUserFromRequest(request);

            Optional<GameData> gameOptional = gameRepository.findGameById(gameId);

            if (validateGame(gameOptional, gameId) != null) {
                return validateGame(gameRepository.findGameById(gameId), gameId);
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
                .rating(wishlistRequest.getRating())
                .status(ListGameStatus.fromStringSwitch(wishlistRequest.getStatus()))
                .build();

        wishlistRepository.save(wishlistEntry);

        log.info("Successfully added game: {} to the wishlist of user: {}", gameData.getName(), appUser.getUsername());
        return ResponseEntity.ok().body(new MessageResponse(MessageType.SUCCESS, "Game successfully added to wishlist"));
    }


    private ResponseEntity<MessageResponse> updateWishlistEntry(final AppUser appUser, final GameData gameData,
                                                                final WishlistRequest wishlistRequest, final Wishlist existingWishlistEntry) {
        log.info("Game: {} already in wishlist of user: {}. Seeking to update wishlist entry", gameData.getName(), appUser.getUsername());

        boolean isChanged = false;

        if (wishlistRequest.getRating() == null) {
            log.info("Removing rating for game: {} in wishlist of user: {}", gameData.getName(), appUser.getUsername());
            existingWishlistEntry.setRating(null);
            isChanged = true;
        }

        if (wishlistRequest.getRating() != null && !Objects.equals(wishlistRequest.getRating(), existingWishlistEntry.getRating())) {
            log.info("Updating rating of game: {} in wishlist of user: {}", gameData.getName(), appUser.getUsername());
            existingWishlistEntry.setRating(wishlistRequest.getRating());
            isChanged = true;
        }


        if (wishlistRequest.getStatus() != null && ListGameStatus.fromStringSwitch(wishlistRequest.getStatus()) != existingWishlistEntry.getStatus()) {
            log.info("Updating status of game: {} in wishlist of user: {}", gameData.getName(), appUser.getUsername());
            existingWishlistEntry.setStatus(ListGameStatus.fromStringSwitch(wishlistRequest.getStatus()));
            isChanged = true;
        }

        if (isChanged) {
            log.info("Updated Wishlist Entry for game with id: {}  for user: {}",
                    gameData.getId(), appUser.getUsername());
            wishlistRepository.save(existingWishlistEntry);
        } else {
            log.info("No changes to Wishlist Entry for game with id: {} for user: {} so no updating",
                    gameData.getId(), appUser.getUsername());
        }

        return ResponseEntity.ok().body(new MessageResponse(MessageType.SUCCESS, "Game successfully updated in wishlist"));
    }

    public ResponseEntity<MessageResponse> updateRating(HttpServletRequest request, Long gameId, RatingRequest ratingRequest) {
        try {
            log.info("Rating game with id {} in wishlist", gameId);
            final GameData gameData = gameRepository.findGameById(gameId).orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Game not found"));

            final AppUser appUser = getAppUserFromRequest(request);

            log.info("Found user: {} and game: {}. Finding wishlist entry", appUser.getUsername(), gameData.getName());

            final Optional<Wishlist> existingWishlistEntryOptional = wishlistRepository
                    .findByUserinfoIdAndGameinfoId(appUser.getId(), gameData.getId());

            if (existingWishlistEntryOptional.isEmpty()) {
                log.info("Game: {} not in wishlist of user: {}. Cannot change rating", gameData.getName(), appUser.getUsername());
                return ResponseEntity.badRequest().body(new MessageResponse(MessageType.ERROR, "Game not in wishlist"));
            }

            final Wishlist existingWishlistEntry = existingWishlistEntryOptional.get();
            log.info(String.valueOf(ratingRequest.getRating()));
            log.info("Found user: {} and game: {}. Changing rating", appUser.getUsername(), gameData.getName());
            existingWishlistEntry.setRating(ratingRequest.getRating());
            wishlistRepository.save(existingWishlistEntry);

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

    public ResponseEntity<MessageResponse> updateStatus(HttpServletRequest request, Long gameId, String status) {
        try {
            log.info("Updating game status");
            final GameData gameData = gameRepository.findGameById(gameId).orElseThrow(() ->
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


}
