package com.pelycan.api.service;

import com.pelycan.api.domain.User;
import com.pelycan.api.domain.UserRole;
import com.pelycan.api.domain.PasswordResetToken;
import com.pelycan.api.repository.PasswordResetTokenRepository;
import com.pelycan.api.repository.UserRepository;
import com.pelycan.api.security.JwtService;
import com.pelycan.api.web.dto.AuthPayload;
import com.pelycan.api.web.dto.ForgotPasswordRequest;
import com.pelycan.api.web.dto.LoginRequest;
import com.pelycan.api.web.dto.RegisterRequest;
import com.pelycan.api.web.dto.ResetPasswordRequest;
import com.pelycan.api.web.dto.UserDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private static final int RESET_CODE_LENGTH = 6;
    private static final int RESET_CODE_TTL_MINUTES = 30;

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final SecureRandom secureRandom = new SecureRandom();

    public AuthService(
            UserRepository userRepository,
            PasswordResetTokenRepository passwordResetTokenRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthPayload register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Un compte existe déjà avec cet email");
        }

        User user = new User();
        user.setFirstName(request.firstName().trim());
        user.setLastName(request.lastName().trim());
        user.setEmail(request.email().trim().toLowerCase());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setPhoneNumber(request.phone());
        user.setRole(UserRole.user);
        user.setStatus("active");
        userRepository.save(user);

        String token = jwtService.createToken(user.getId(), user.getRole().name());
        return new AuthPayload(toDto(user), token, redirectFor(user.getRole()));
    }

    @Transactional
    public AuthPayload login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email().trim().toLowerCase())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Email ou mot de passe incorrect"));

        if (user.getPassword() == null || !passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Email ou mot de passe incorrect");
        }

        user.setLastLogin(Instant.now());
        userRepository.save(user);

        String token = jwtService.createToken(user.getId(), user.getRole().name());
        return new AuthPayload(toDto(user), token, redirectFor(user.getRole()));
    }

    public UserDto verify(User user) {
        return toDto(user);
    }

    @Transactional
    public String requestPasswordReset(ForgotPasswordRequest request) {
        String email = request.email().trim().toLowerCase();
        userRepository.findByEmail(email).ifPresent(user -> {
            passwordResetTokenRepository.invalidateActiveTokensForUser(user, Instant.now());

            String code = generateResetCode();
            PasswordResetToken token = new PasswordResetToken();
            token.setUser(user);
            token.setCodeHash(passwordEncoder.encode(code));
            token.setExpiresAt(Instant.now().plus(RESET_CODE_TTL_MINUTES, ChronoUnit.MINUTES));
            passwordResetTokenRepository.save(token);

            log.info("Code de réinitialisation pour {} : {} (valide {} min)", email, code, RESET_CODE_TTL_MINUTES);
        });

        return "Si un compte existe avec cet email, un code de réinitialisation a été envoyé.";
    }

    @Transactional
    public String resetPassword(ResetPasswordRequest request) {
        User user = userRepository.findByEmail(request.email().trim().toLowerCase())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Code invalide ou expiré"));

        List<PasswordResetToken> tokens = passwordResetTokenRepository.findActiveTokensForUser(user, Instant.now());
        PasswordResetToken matched = tokens.stream()
                .filter(token -> passwordEncoder.matches(request.code(), token.getCodeHash()))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Code invalide ou expiré"));

        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);

        matched.setUsedAt(Instant.now());
        passwordResetTokenRepository.save(matched);
        passwordResetTokenRepository.invalidateActiveTokensForUser(user, Instant.now());

        return "Mot de passe mis à jour avec succès.";
    }

    private String generateResetCode() {
        int code = secureRandom.nextInt(1_000_000);
        return String.format("%06d", code);
    }

    private static UserDto toDto(User user) {
        return new UserDto(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getRole(),
                user.getStatus());
    }

    private static String redirectFor(UserRole role) {
        return switch (role) {
            case admin -> "/(admin)/DashboardScreen";
            case pro -> "/(pro)/DashboardScreen";
            default -> "/(app)/HomeScreen";
        };
    }
}
