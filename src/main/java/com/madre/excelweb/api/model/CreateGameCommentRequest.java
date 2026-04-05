package com.madre.excelweb.api.model;

public record CreateGameCommentRequest(
        String content,
        String parentCommentId
) {
}
