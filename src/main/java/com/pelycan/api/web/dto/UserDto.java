package com.pelycan.api.web.dto;

import com.pelycan.api.domain.UserRole;

import java.util.UUID;

public record UserDto(
        UUID id,
        String firstName,
        String lastName,
        String email,
        UserRole role,
        String status
) {}
