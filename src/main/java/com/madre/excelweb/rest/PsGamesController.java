package com.madre.excelweb.rest;

import com.madre.excelweb.api.model.IdResponse;
import com.madre.excelweb.api.model.MessageResponse;
import com.madre.excelweb.api.model.MessageType;
import com.madre.excelweb.api.model.WishlistRequest;
import com.madre.excelweb.jpa.entity.AppUser;
import com.madre.excelweb.jpa.entity.GameData;
import com.madre.excelweb.jpa.entity.Wishlist;
import com.madre.excelweb.jpa.repositories.GameRepository;
import com.madre.excelweb.jpa.repositories.UserRepository;
import com.madre.excelweb.jpa.repositories.WishlistRepository;
import com.madre.excelweb.models.ListGameStatus;
import com.madre.excelweb.security.jwt.JwtUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@Slf4j
@CrossOrigin(value = "*", maxAge = 3600)
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/auth")
public class PsGamesController {
    private final UserRepository userRepository;
    private final GameRepository gameRepository;
    private final WishlistRepository wishlistRepository;
    private final JwtUtils jwtUtils;

    @PostMapping("/addToList")
    public synchronized ResponseEntity<?> addToWishlist(HttpServletRequest request, @RequestBody WishlistRequest wishlistRequest) {
        try {
            log.info("Attempting to add game to wishlist");
            Optional<AppUser> user = getUserInfoById(request);
            Optional<GameData> game = gameRepository.findGameById(wishlistRequest.getGameId());

            if (user.isEmpty() || game.isEmpty()) {
                return ResponseEntity.badRequest().body(new MessageResponse(MessageType.ERROR, "User and/or game not found"));
            }


            final AppUser appUser = user.get();
            final GameData gameData = game.get();


            log.info("Found user: {} and game: {}, adding to wishlist", appUser.getUsername(), gameData.getName());

            final Wishlist existingWishlistEntry = wishlistRepository.findByUserinfoIdAndGameinfoId(appUser.getId(), gameData.getId()).orElse(null);

            if (existingWishlistEntry != null) {
                log.info("Game: {} already in wishlist of user: {}", gameData.getName(), appUser.getUsername());
                return ResponseEntity.badRequest().body(new MessageResponse(MessageType.ERROR, "Game already in wishlist"));
            }


            final Wishlist wishlistEntry = Wishlist.builder()
                    .gameinfo(gameData)
                    .userinfo(appUser)
                    .rating(wishlistRequest.getRating())
                    .status(getListGameStatusFromString(wishlistRequest.getStatus()))
                    .build();

            wishlistRepository.save(wishlistEntry);

            appUser.getWishedGames().add(wishlistEntry);

            log.info("Successfully added game: {} to the wishlist of user: {}", gameData.getName(), appUser.getUsername());


        } catch (Exception e) {
            log.error("Error adding game to wishlist", e);
            return ResponseEntity.badRequest().body(new MessageResponse(MessageType.ERROR, "Error adding game to wishlist. Please try again later"));
        }


        return ResponseEntity.ok().body(new MessageResponse(MessageType.SUCCESS, "Game successfully added to wishlist"));
    }

    @PostMapping("/removeFromList")
    public synchronized ResponseEntity<?> removeFromWishlist(HttpServletRequest request, @RequestBody WishlistRequest wishlistRequest) {
        try {
            log.info("Attempting to remove game from wishlist");
            Optional<AppUser> user = getUserInfoById(request);
            Optional<GameData> game = gameRepository.findGameById(wishlistRequest.getGameId());

            if (user.isEmpty() || game.isEmpty()) {
                return ResponseEntity.badRequest().body(new MessageResponse(MessageType.ERROR, "User and/or game not found"));
            }

            final AppUser appUser = user.get();
            final GameData gameData = game.get();
            log.info("Found user: {} and game: {}, removing from wishlist", appUser.getUsername(), gameData.getName());

            final Wishlist existingWishlistEntry = wishlistRepository.findByUserinfoIdAndGameinfoId(appUser.getId(), gameData.getId()).orElse(null);

            if (existingWishlistEntry == null) {
                log.info("Game: {} not in wishlist of user: {}", gameData.getName(), appUser.getUsername());
                return ResponseEntity.badRequest().body(new MessageResponse(MessageType.ERROR, "Game not in wishlist"));
            }

            wishlistRepository.delete(existingWishlistEntry);
            appUser.getWishedGames().remove(existingWishlistEntry);

            log.info("Successfully removed game: {} from the wishlist of user: {}", gameData.getName(), appUser.getUsername());


        } catch (Exception e) {
            log.error("Error removing game from wishlist", e);
            return ResponseEntity.badRequest().body(new MessageResponse(MessageType.ERROR, "Error removing game from wishlist. Please try again later"));
        }

        return ResponseEntity.ok().body(new MessageResponse(MessageType.SUCCESS, "Game successfully removed from wishlist"));
    }

    @PostMapping("/rateGame")
    public synchronized ResponseEntity<?> rateGame(HttpServletRequest request, @RequestBody WishlistRequest wishlistRequest) {
        try {
            log.info("Attempting to rate game");
            Optional<AppUser> user = getUserInfoById(request);
            Optional<GameData> game = gameRepository.findGameById(wishlistRequest.getGameId());

            if (user.isEmpty() || game.isEmpty()) {
                return ResponseEntity.badRequest().body(new MessageResponse(MessageType.ERROR, "User and/or game not found"));
            }

            final AppUser appUser = user.get();
            final GameData gameData = game.get();
            log.info("Found user: {} and game: {}, rating game", appUser.getUsername(), gameData.getName());

            final Wishlist existingWishlistEntry = wishlistRepository.findByUserinfoIdAndGameinfoId(appUser.getId(), gameData.getId()).orElse(null);

            if (existingWishlistEntry == null) {
                log.info("Game: {} not in wishlist of user: {}", gameData.getName(), appUser.getUsername());
                return ResponseEntity.badRequest().body(new MessageResponse(MessageType.ERROR, "Game not in wishlist"));
            }

            existingWishlistEntry.setRating(wishlistRequest.getRating());
            wishlistRepository.save(existingWishlistEntry);

            log.info("Successfully rated game: {} from the wishlist of user: {}", gameData.getName(), appUser.getUsername());
        } catch (Exception e) {
            log.error("Error rating game", e);
            return ResponseEntity.badRequest().body(new MessageResponse(MessageType.ERROR, "Error occurred while attempting to rate game. Please try again later"));
        }
        return ResponseEntity.ok().body(new MessageResponse(MessageType.SUCCESS, "Game successfully rated"));
    }

    @GetMapping("/games/getList")
    public ResponseEntity<?> getWishlist(HttpServletRequest request) {
        try {


            log.info("Attempting to get wishlist");
            Optional<AppUser> user = getUserInfoById(request);

            if (user.isEmpty()) {
                return ResponseEntity.badRequest().body(new MessageResponse(MessageType.ERROR, "User not found"));
            }

            final AppUser appUser = user.get();
            log.info("Found user: {}, getting wishlist", appUser.getUsername());
            return ResponseEntity.ok().body(wishlistRepository.findByUserinfoId(appUser.getId()));

        } catch (Exception e) {
            log.error("Error getting wishlist", e);
            return ResponseEntity.badRequest().body(new MessageResponse(MessageType.ERROR, "Error getting wishlist. Please try again later"));
        }
    }

    @GetMapping("/games/{gameId}")
    public ResponseEntity<GameData> retrieveGame(@PathVariable Long gameId) {
        try {
            log.info("Attempting to get game with id {}", gameId);
            Optional<GameData> game = gameRepository.findGameById(gameId);

            if (game.isEmpty()) {
                log.info("Game with id {} not found", gameId);
                return ResponseEntity.notFound().build();
            }

            log.info("Successfully retrieved game with id : {}.", gameId);
            return ResponseEntity.ok().body(game.get());

        } catch (Exception e) {
            log.error("Error getting game, please reload and try again", e);
            return ResponseEntity.badRequest().build();
        }

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

    private Optional<AppUser> getUserInfoById(HttpServletRequest request) {
        final String jwt = jwtUtils.getJwtFromCookies(request);
        final Long userId = jwtUtils.getUserIdFromToken(jwt);

        log.info("Attempting to get wishlist");
        return userRepository.getUserById(userId);
    }

    private ListGameStatus getListGameStatusFromString(final String status) {
        return Optional.ofNullable(status)
                .map(String::toLowerCase)
                .map(s -> switch (s) {
                    case "completed" -> ListGameStatus.COMPLETED;
                    case "playing" -> ListGameStatus.PLAYING;
                    case "dropped" -> ListGameStatus.DROPPED;
                    case "plan to play" -> ListGameStatus.PLAN_TO_PLAY;
                    case "on hold" -> ListGameStatus.ON_HOLD;
                    case "replaying" -> ListGameStatus.REPLAYING;
                    case "watching" -> ListGameStatus.WATCHING;
                    default -> null;
                })
                .orElse(null);
    }

    private ListGameStatus getListGameStatusFromNumb(final int numbVal) {
        switch (numbVal) {
            case 1:
                return ListGameStatus.COMPLETED;
            case 2:
                return ListGameStatus.PLAYING;
            case 3:
                return ListGameStatus.DROPPED;
            case 4:
                return ListGameStatus.PLAN_TO_PLAY;
            case 5:
                return ListGameStatus.ON_HOLD;
            case 6:
                return ListGameStatus.REPLAYING;
            case 7:
                return ListGameStatus.WATCHING;
            default:
                return null;
        }
    }
}
