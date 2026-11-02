package com.pelycan.api.web.dto;

import com.pelycan.api.domain.ActualiteCategory;
import com.pelycan.api.domain.ActualiteStatus;

import java.time.Instant;
import java.util.UUID;

public record ActualiteAdminDto(
        UUID id,
        String title,
        String excerpt,
        String content,
        ActualiteCategory category,
        ActualiteStatus status,
        Instant publishedAt,
        String linkUrl,
        String linkLabel,
        Instant createdAt,
        Instant updatedAt,
        AuthorSummary author
) {
    public record AuthorSummary(UUID id, String firstName, String lastName) {}
}
