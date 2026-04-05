package com.madre.excelweb.service.util;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public final class ImageUrlValidationUtils {
    private ImageUrlValidationUtils() {
    }

    public static String normalizeOptionalImageUrl(final String rawValue,
                                                   final String fieldLabel,
                                                   final int maxRemoteLength,
                                                   final int maxInlineLength,
                                                   final int maxInlineGifBytes) {
        if (rawValue == null) {
            return null;
        }

        final String sanitized = rawValue.trim();
        if (sanitized.isEmpty()) {
            return null;
        }

        if (sanitized.startsWith("data:image/")) {
            if (!sanitized.matches("^data:image/(png|jpeg|jpg|webp|gif);base64,.+")) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        fieldLabel + " uploads must be png, jpg, gif, or webp images.");
            }
            if (sanitized.startsWith("data:image/gif;")) {
                if (estimateDecodedBytes(sanitized) > maxInlineGifBytes) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            fieldLabel + " GIF uploads must be 30 MB or smaller.");
                }
            } else if (sanitized.length() > maxInlineLength) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, fieldLabel + " upload is too large.");
            }
            return sanitized;
        }

        if (sanitized.startsWith("https://") || sanitized.startsWith("http://")) {
            if (sanitized.length() > maxRemoteLength) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, fieldLabel + " URL is too long.");
            }
            return sanitized;
        }

        if (sanitized.startsWith("/api/media/")) {
            if (sanitized.length() > maxRemoteLength) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, fieldLabel + " URL is too long.");
            }
            return sanitized;
        }

        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                fieldLabel + " must be an uploaded image or an http(s) URL.");
    }

    private static int estimateDecodedBytes(final String dataUrl) {
        final int commaIndex = dataUrl.indexOf(',');
        if (commaIndex < 0 || commaIndex == dataUrl.length() - 1) {
            return 0;
        }

        final String base64 = dataUrl.substring(commaIndex + 1);
        int padding = 0;
        if (base64.endsWith("==")) {
            padding = 2;
        } else if (base64.endsWith("=")) {
            padding = 1;
        }

        return (base64.length() * 3 / 4) - padding;
    }
}
