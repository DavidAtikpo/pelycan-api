package com.pelycan.api.web.dto;

import com.pelycan.api.domain.ActualiteCategory;

import java.time.Instant;
import java.util.UUID;

public record ActualitePublicDto(
        UUID id,
        String title,
        String excerpt,
        ActualiteCategory category,
        Instant date,
        String href,
        String hrefLabel
) {}
