package com.madre.excelweb.rest;

import com.madre.excelweb.api.model.MessageResponse;
import com.madre.excelweb.api.model.MessageType;
import com.madre.excelweb.api.model.dto.GameDto;
import com.madre.excelweb.api.model.dto.WishlistDto;
import com.madre.excelweb.jpa.entity.AppUser;
import com.madre.excelweb.jpa.entity.GameData;
import com.madre.excelweb.jpa.entity.Wishlist;
import com.madre.excelweb.jpa.repositories.UserRepository;
import com.madre.excelweb.jpa.repositories.WishlistRepository;
import com.madre.excelweb.security.jwt.AuthTokenFilter;
import com.madre.excelweb.security.jwt.JwtUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;

import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
public class ControllerUtils {
    private final JwtUtils jwtUtils;
    private final WishlistRepository wishlistRepository;
    private final UserRepository userRepository;

    public static AppUser getAppUserFromRequest(HttpServletRequest request) {
        return (AppUser) request.getAttribute(AuthTokenFilter.USER_INFO_ATTRIBUTE);
    }

    public static ResponseEntity<MessageResponse> validateGame(final Optional<GameData> game, Long gameId) {

        if (game.isEmpty()) {
            log.error("Game with id {} not found", gameId);
            return ResponseEntity.badRequest().body(new MessageResponse(MessageType.ERROR, "Game not found"));
        }
        return null;
    }

    protected Boolean gameInWishlist(final AppUser appUser, final GameData gameData) {
        final Wishlist existingWishlistEntry = wishlistRepository.findByUserinfoIdAndGameinfoId(appUser.getId(), gameData.getId()).orElse(null);
        return existingWishlistEntry != null;
    }

    protected Optional<AppUser> getUserInfoById(HttpServletRequest request) {
        final String jwt = jwtUtils.getJwtFromCookies(request);
        final Long userId = jwtUtils.getUserIdFromToken(jwt);


        return userRepository.getUserById(userId);
    }

    protected GameDto gameDataToGameDto(final AppUser appUser, final GameData gameData) {

        return GameDto.builder()
                .id(gameData.getId().toString())
                .name(gameData.getName())
                .psnId(gameData.getPsnId())
                .url(gameData.getUrl())
                .price(gameData.getPrice())
                .releaseDate(gameData.getReleaseDate())
                .description(gameData.getDescription())
                .categories(gameData.getCategories())
                .platforms(gameData.getPlatforms())
                .provider(gameData.getProviderName())
                .imageUrl(gameData.getImageUrl())
                .inWishlist(gameInWishlist(appUser, gameData))
                .build();
    }

    protected WishlistDto wishlistEntryToWishlistDto(final Wishlist wishlist) {
        return WishlistDto.builder()
                .gameinfo(gameDataToGameDto(wishlist.getUserinfo(), wishlist.getGameinfo()))
                .id(wishlist.getId())
                .rating(wishlist.getRating())
                .status(wishlist.getStatus())
                .build();
    }

}
