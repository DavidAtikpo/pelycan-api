package com.pelycan.api.service;

import com.pelycan.api.config.AgoraProperties;
import io.agora.media.RtcTokenBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AgoraTokenService {

    private final AgoraProperties properties;

    public AgoraTokenService(AgoraProperties properties) {
        this.properties = properties;
    }

    public String buildPublisherToken(String channelName, int uid) {
        if (!StringUtils.hasText(properties.appId()) || !StringUtils.hasText(properties.appCertificate())) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Agora non configuré");
        }
        int expire = (int) (System.currentTimeMillis() / 1000L + properties.tokenTtlSeconds());
        RtcTokenBuilder builder = new RtcTokenBuilder();
        return builder.buildTokenWithUid(
                properties.appId(),
                properties.appCertificate(),
                channelName,
                uid,
                RtcTokenBuilder.Role.Role_Publisher,
                expire);
    }

    public static int agoraUidFromUser(java.util.UUID userId) {
        String hex = userId.toString().replace("-", "").substring(0, 8);
        return (int) Long.parseLong(hex, 16);
    }
}
