package com.madre.excelweb.service;

import com.madre.excelweb.api.model.UpdatePasswordRequest;
import com.madre.excelweb.api.model.UpdateUserProfileRequest;
import com.madre.excelweb.api.model.dto.UserProfileDto;
import com.madre.excelweb.jpa.entity.AppUser;
import com.madre.excelweb.jpa.repositories.UserRepository;
import com.madre.excelweb.models.MediaPurpose;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class AccountService {
    private static final int MAX_BIO_LENGTH = 600;
    private static final int MAX_REMOTE_AVATAR_LENGTH = 2048;
    private static final int MAX_INLINE_AVATAR_LENGTH = 1_000_000;
    private static final int MAX_INLINE_GIF_AVATAR_BYTES = 30 * 1024 * 1024;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final MediaAssetService mediaAssetService;

    @Transactional(readOnly = true)
    public UserProfileDto getProfile(final AppUser appUser) {
        return UserProfileDto.fromEntity(requireUser(appUser));
    }

    @Transactional
    public UserProfileDto updateProfile(final AppUser appUser, final UpdateUserProfileRequest request) {
        final AppUser user = requireUser(appUser);

        final String displayName = sanitizeOptional(request.displayName());
        final String bio = sanitizeOptional(request.bio());
        final String favoritePlatform = sanitizeOptional(request.favoritePlatform());
        final String avatarUrl = normalizeAvatar(request.avatarUrl(), user.getId());

        if (displayName != null && displayName.length() < 2) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Display name must be at least 2 characters long.");
        }
        if (displayName != null && displayName.length() > 120) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Display name must be 120 characters or fewer.");
        }
        if (bio != null && bio.length() > MAX_BIO_LENGTH) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Bio must be 600 characters or fewer.");
        }
        if (favoritePlatform != null && favoritePlatform.length() > 80) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Favorite platform must be 80 characters or fewer.");
        }

        user.setDisplayName(displayName);
        user.setBio(bio);
        user.setFavoritePlatform(favoritePlatform);
        user.setAvatarUrl(avatarUrl);

        return UserProfileDto.fromEntity(userRepository.save(user));
    }

    public void updatePassword(final AppUser appUser, final UpdatePasswordRequest request) {
        final AppUser user = requireUser(appUser);
        if (request == null || request.currentPassword() == null || request.newPassword() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Current and new passwords are required.");
        }
        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Current password is incorrect.");
        }
        if (request.newPassword().length() < 8) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "New password must be at least 8 characters long.");
        }
        if (request.newPassword().equals(request.currentPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "New password must be different from the current password.");
        }

        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
    }

    private AppUser requireUser(final AppUser appUser) {
        if (appUser == null || appUser.getId() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found.");
        }
        return userRepository.getUserById(appUser.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found."));
    }

    private String normalizeAvatar(final String avatarUrl, final Long createdByUserId) {
        return mediaAssetService.normalizeImageReference(
                avatarUrl,
                "Avatar",
                MediaPurpose.AVATAR,
                createdByUserId,
                MAX_REMOTE_AVATAR_LENGTH,
                MAX_INLINE_AVATAR_LENGTH,
                MAX_INLINE_GIF_AVATAR_BYTES
        );
    }

    private String sanitizeOptional(final String value) {
        if (value == null) {
            return null;
        }
        final String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
