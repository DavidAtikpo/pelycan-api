package com.pelycan.api.service;

import com.pelycan.api.domain.UserFcmToken;
import com.pelycan.api.repository.UserFcmTokenRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class FcmTokenService {

    private final UserFcmTokenRepository repository;

    public FcmTokenService(UserFcmTokenRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void register(UUID userId, String fcmToken, String platform) {
        Optional<UserFcmToken> existing = repository.findByUserIdAndToken(userId, fcmToken);
        if (existing.isPresent()) {
            UserFcmToken row = existing.get();
            row.setUpdatedAt(Instant.now());
            if (platform != null && !platform.isBlank()) {
                row.setPlatform(platform);
            }
            repository.save(row);
            return;
        }
        repository.deleteByUserId(userId);
        UserFcmToken token = new UserFcmToken();
        token.setUserId(userId);
        token.setToken(fcmToken);
        token.setPlatform(platform);
        repository.save(token);
    }
}
