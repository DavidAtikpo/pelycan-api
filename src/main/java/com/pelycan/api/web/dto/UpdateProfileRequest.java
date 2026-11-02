package com.pelycan.api.web.dto;

public record UpdateProfileRequest(
        String firstName,
        String lastName,
        String phone,
        String profilePicture
) {}
