package com.madre.excelweb.api.model;

public record VerificationResponse(
        MessageType type,
        String message,
        boolean emailSent,
        String verificationUrl
) {
}
