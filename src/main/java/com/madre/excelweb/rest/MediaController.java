package com.madre.excelweb.rest;

import com.madre.excelweb.api.model.dto.MediaAssetDto;
import com.madre.excelweb.jpa.entity.AppUser;
import com.madre.excelweb.jpa.entity.MediaAsset;
import com.madre.excelweb.models.MediaPurpose;
import com.madre.excelweb.service.MediaAssetService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.concurrent.TimeUnit;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/media")
public class MediaController {
    private final MediaAssetService mediaAssetService;

    @PostMapping("/upload")
    public ResponseEntity<MediaAssetDto> uploadMedia(final HttpServletRequest request,
                                                     @RequestParam(required = false) final String purpose,
                                                     @RequestParam("file") final MultipartFile file) {
        final AppUser appUser = ControllerUtils.getAppUserFromRequest(request);
        return ResponseEntity.ok(mediaAssetService.upload(appUser, MediaPurpose.fromNullable(purpose), file));
    }

    @GetMapping("/{publicId}")
    public ResponseEntity<Resource> getMediaAsset(@PathVariable final String publicId) {
        final MediaAsset mediaAsset = mediaAssetService.getMediaAssetEntity(publicId);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(30, TimeUnit.DAYS).cachePublic())
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + publicId + "\"")
                .contentType(MediaType.parseMediaType(mediaAsset.getContentType()))
                .body(mediaAssetService.loadAsResource(publicId));
    }
}
