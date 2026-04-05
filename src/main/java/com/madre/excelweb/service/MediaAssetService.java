package com.madre.excelweb.service;

import com.madre.excelweb.api.model.dto.MediaAssetDto;
import com.madre.excelweb.jpa.entity.AppUser;
import com.madre.excelweb.jpa.entity.MediaAsset;
import com.madre.excelweb.jpa.repositories.MediaAssetRepository;
import com.madre.excelweb.models.MediaPurpose;
import com.madre.excelweb.service.util.ImageUrlValidationUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MediaAssetService {
    private static final long MAX_UPLOAD_BYTES = 35L * 1024L * 1024L;
    private static final Map<String, String> CONTENT_TYPE_TO_EXTENSION = Map.of(
            "image/png", "png",
            "image/jpeg", "jpg",
            "image/jpg", "jpg",
            "image/webp", "webp",
            "image/gif", "gif"
    );

    private final MediaAssetRepository mediaAssetRepository;

    @Value("${web.app.media.storage-path:./var/media}")
    private String storagePathRoot;

    @Value("${web.app.media.public-base-path:/api/media}")
    private String publicBasePath;

    @Transactional
    public MediaAssetDto upload(final AppUser appUser,
                                final MediaPurpose purpose,
                                final MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A media file is required.");
        }

        final String contentType = normalizeContentType(file.getContentType());
        final long size = file.getSize();
        if (size <= 0 || size > MAX_UPLOAD_BYTES) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Uploaded media must be 35 MB or smaller.");
        }

        final String extension = extensionFor(contentType);
        final String publicId = UUID.randomUUID().toString();
        final Path targetPath = buildStoragePath(purpose, publicId, extension);

        try {
            Files.createDirectories(targetPath.getParent());
            file.transferTo(targetPath);
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not store uploaded media.", exception);
        }

        final MediaAsset mediaAsset = mediaAssetRepository.save(MediaAsset.builder()
                .publicId(publicId)
                .purpose(purpose)
                .contentType(contentType)
                .originalFilename(StringUtils.hasText(file.getOriginalFilename()) ? file.getOriginalFilename().trim() : null)
                .storagePath(targetPath.toString())
                .sizeBytes(size)
                .createdByUserId(appUser == null ? null : appUser.getId())
                .createdAt(OffsetDateTime.now(ZoneOffset.UTC))
                .build());

        return MediaAssetDto.fromEntity(mediaAsset, buildPublicUrl(publicId));
    }

    @Transactional
    public String normalizeImageReference(final String rawValue,
                                          final String fieldLabel,
                                          final MediaPurpose purpose,
                                          final Long createdByUserId,
                                          final int maxRemoteLength,
                                          final int maxInlineLength,
                                          final int maxInlineGifBytes) {
        final String normalized = ImageUrlValidationUtils.normalizeOptionalImageUrl(
                rawValue,
                fieldLabel,
                maxRemoteLength,
                maxInlineLength,
                maxInlineGifBytes
        );
        if (normalized == null) {
            return null;
        }
        if (normalized.startsWith("/api/media/")) {
            return normalized;
        }
        if (!normalized.startsWith("data:image/")) {
            return normalized;
        }

        final ParsedDataUrl parsedDataUrl = parseDataUrl(normalized);
        final String publicId = UUID.randomUUID().toString();
        final Path targetPath = buildStoragePath(purpose, publicId, extensionFor(parsedDataUrl.contentType()));

        try {
            Files.createDirectories(targetPath.getParent());
            Files.write(targetPath, parsedDataUrl.bytes());
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not store uploaded media.", exception);
        }

        mediaAssetRepository.save(MediaAsset.builder()
                .publicId(publicId)
                .purpose(purpose)
                .contentType(parsedDataUrl.contentType())
                .originalFilename(purpose.name().toLowerCase() + "-upload." + extensionFor(parsedDataUrl.contentType()))
                .storagePath(targetPath.toString())
                .sizeBytes((long) parsedDataUrl.bytes().length)
                .createdByUserId(createdByUserId)
                .createdAt(OffsetDateTime.now(ZoneOffset.UTC))
                .build());

        return buildPublicUrl(publicId);
    }

    @Transactional(readOnly = true)
    public MediaAssetDto getMediaAsset(final String publicId) {
        final MediaAsset mediaAsset = getMediaAssetEntity(publicId);
        return MediaAssetDto.fromEntity(mediaAsset, buildPublicUrl(mediaAsset.getPublicId()));
    }

    @Transactional(readOnly = true)
    public MediaAsset getMediaAssetEntity(final String publicId) {
        return mediaAssetRepository.findByPublicId(publicId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Media asset not found."));
    }

    @Transactional(readOnly = true)
    public Resource loadAsResource(final String publicId) {
        final MediaAsset mediaAsset = getMediaAssetEntity(publicId);
        final Path path = Paths.get(mediaAsset.getStoragePath()).normalize();
        if (!Files.exists(path)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Media asset file not found.");
        }
        return new FileSystemResource(path);
    }

    public String buildPublicUrl(final String publicId) {
        return publicBasePath.endsWith("/")
                ? publicBasePath + publicId
                : publicBasePath + "/" + publicId;
    }

    private Path buildStoragePath(final MediaPurpose purpose, final String publicId, final String extension) {
        final String normalizedRoot = storagePathRoot == null || storagePathRoot.isBlank() ? "./var/media" : storagePathRoot.trim();
        return Paths.get(normalizedRoot)
                .toAbsolutePath()
                .normalize()
                .resolve(purpose.name().toLowerCase())
                .resolve(publicId + "." + extension)
                .normalize();
    }

    private String normalizeContentType(final String rawContentType) {
        final String contentType = rawContentType == null ? "" : rawContentType.trim().toLowerCase();
        if (!CONTENT_TYPE_TO_EXTENSION.containsKey(contentType)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only png, jpg, webp, and gif images are supported.");
        }
        return contentType;
    }

    private String extensionFor(final String contentType) {
        final String extension = CONTENT_TYPE_TO_EXTENSION.get(contentType);
        if (extension == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported media type.");
        }
        return extension;
    }

    private ParsedDataUrl parseDataUrl(final String dataUrl) {
        final int commaIndex = dataUrl.indexOf(',');
        if (commaIndex <= 0 || commaIndex == dataUrl.length() - 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Uploaded image data is invalid.");
        }

        final String metadata = dataUrl.substring(5, commaIndex);
        final String base64Payload = dataUrl.substring(commaIndex + 1);
        final String contentType = normalizeContentType(metadata.substring(0, metadata.indexOf(';')));

        try {
            return new ParsedDataUrl(contentType, Base64.getDecoder().decode(base64Payload));
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Uploaded image data is invalid.", exception);
        }
    }

    private record ParsedDataUrl(String contentType, byte[] bytes) {
    }
}
