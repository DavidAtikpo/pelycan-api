package com.pelycan.api.web;

import com.pelycan.api.domain.User;
import com.pelycan.api.service.AuthService;
import com.pelycan.api.web.dto.ApiResponse;
import com.pelycan.api.web.dto.AuthPayload;
import com.pelycan.api.web.dto.ForgotPasswordRequest;
import com.pelycan.api.web.dto.LoginRequest;
import com.pelycan.api.web.dto.RegisterRequest;
import com.pelycan.api.web.dto.ResetPasswordRequest;
import com.pelycan.api.web.dto.UserDto;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ApiResponse<AuthPayload> register(@Valid @RequestBody RegisterRequest request) {
        AuthPayload payload = authService.register(request);
        return ApiResponse.ok(payload);
    }

    @PostMapping("/login")
    public ApiResponse<AuthPayload> login(@Valid @RequestBody LoginRequest request) {
        AuthPayload payload = authService.login(request);
        return ApiResponse.ok(payload);
    }

    @GetMapping("/verify")
    public ApiResponse<Map<String, Object>> verify(@AuthenticationPrincipal User user) {
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token d'authentification manquant");
        }
        UserDto dto = authService.verify(user);
        return ApiResponse.ok(Map.of(
                "user", dto,
                "message", "Token valide"));
    }

    @PostMapping("/forgot-password")
    public ApiResponse<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        String message = authService.requestPasswordReset(request);
        return ApiResponse.okMessage(message);
    }

    @PostMapping("/reset-password")
    public ApiResponse<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        String message = authService.resetPassword(request);
        return ApiResponse.okMessage(message);
    }
}
