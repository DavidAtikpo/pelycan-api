package com.pelycan.api.web.dto;

public record AuthPayload(UserDto user, String token, String redirectPath) {}
