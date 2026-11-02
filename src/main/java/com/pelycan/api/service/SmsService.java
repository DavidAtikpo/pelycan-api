package com.pelycan.api.service;

import com.pelycan.api.config.SmsProperties;
import com.pelycan.api.util.PhoneNormalizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Service
public class SmsService {

    private static final Logger log = LoggerFactory.getLogger(SmsService.class);

    private final SmsProperties properties;
    private final RestClient restClient;

    public SmsService(SmsProperties properties) {
        this.properties = properties;
        this.restClient = RestClient.create();
    }

    public boolean isConfigured() {
        return properties.isConfigured();
    }

    public SmsResult send(String phone, String body) {
        if (!properties.isConfigured()) {
            return SmsResult.skipped("SMS non configuré");
        }
        String to = PhoneNormalizer.normalize(phone, properties.defaultCountryCode());
        if (to == null) {
            return SmsResult.failure("Numéro invalide");
        }
        if (body == null || body.isBlank()) {
            return SmsResult.failure("Message vide");
        }
        try {
            MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
            form.add("To", to);
            form.add("From", properties.fromNumber().trim());
            form.add("Body", body.trim());

            restClient.post()
                    .uri("https://api.twilio.com/2010-04-01/Accounts/"
                            + properties.accountSid().trim() + "/Messages.json")
                    .headers(h -> h.setBasicAuth(
                            properties.accountSid().trim(),
                            properties.authToken().trim()))
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .toBodilessEntity();

            log.info("SMS envoyé vers {}", maskPhone(to));
            return SmsResult.success();
        } catch (RestClientResponseException e) {
            log.warn("Échec envoi SMS vers {}: {} {}", maskPhone(to), e.getStatusCode(), e.getResponseBodyAsString());
            return SmsResult.failure("Erreur SMS " + e.getStatusCode().value());
        } catch (Exception e) {
            log.warn("Échec envoi SMS vers {}: {}", maskPhone(to), e.getMessage());
            return SmsResult.failure(e.getMessage() != null ? e.getMessage() : "Erreur SMS");
        }
    }

    private static String maskPhone(String phone) {
        if (phone == null || phone.length() < 4) {
            return "****";
        }
        return phone.substring(0, Math.min(4, phone.length())) + "****";
    }

    public record SmsResult(boolean sent, boolean skipped, String error) {
        static SmsResult success() {
            return new SmsResult(true, false, null);
        }

        static SmsResult skipped(String reason) {
            return new SmsResult(false, true, reason);
        }

        static SmsResult failure(String error) {
            return new SmsResult(false, false, error);
        }
    }
}
