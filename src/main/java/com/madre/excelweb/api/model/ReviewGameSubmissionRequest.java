package com.madre.excelweb.api.model;

public record ReviewGameSubmissionRequest(
        String status,
        String reviewNotes
) {
}
