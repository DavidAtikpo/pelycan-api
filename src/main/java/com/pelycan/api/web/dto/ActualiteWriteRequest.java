package com.pelycan.api.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.pelycan.api.domain.ActualiteCategory;
import com.pelycan.api.domain.ActualiteStatus;

public record ActualiteWriteRequest(
        String title,
        String excerpt,
        String content,
        ActualiteCategory category,
        ActualiteStatus status,
        @JsonProperty("link_url") String linkUrl,
        @JsonProperty("link_label") String linkLabel
) {}
