package com.madre.excelweb.api.model;

public record UpdatePasswordRequest(String currentPassword, String newPassword) {
}
