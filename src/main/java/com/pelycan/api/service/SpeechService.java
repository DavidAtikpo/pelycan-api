package com.pelycan.api.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.pelycan.api.config.SpeechProperties;
import com.pelycan.api.web.dto.SpeechTranscribeResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Base64;
import java.util.Locale;

@Service
public class SpeechService {

    private static final int MAX_BYTES = 10 * 1024 * 1024;

    private final SpeechProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();

    public SpeechService(SpeechProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    public SpeechTranscribeResponse transcribe(MultipartFile file, String language) {
        if (properties.apiKey() == null || properties.apiKey().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Transcription vocale non configurée (GOOGLE_SPEECH_API_KEY manquante)");
        }
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Fichier audio vide");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Fichier audio trop volumineux (max 10 Mo)");
        }

        String lang = (language == null || language.isBlank())
                ? properties.defaultLanguage()
                : language;

        try {
            byte[] audioBytes = file.getBytes();
            String encoding = resolveEncoding(file.getContentType(), file.getOriginalFilename());

            ObjectNode config = objectMapper.createObjectNode();
            config.put("languageCode", lang);
            config.put("encoding", encoding);
            if ("LINEAR16".equals(encoding)) {
                config.put("sampleRateHertz", 16000);
            }
            config.put("enableAutomaticPunctuation", true);

            ObjectNode audioNode = objectMapper.createObjectNode();
            audioNode.put("content", Base64.getEncoder().encodeToString(audioBytes));

            ObjectNode body = objectMapper.createObjectNode();
            body.set("config", config);
            body.set("audio", audioNode);

            String url = "https://speech.googleapis.com/v1/speech:recognize?key=" + properties.apiKey();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json; charset=utf-8")
                    .timeout(Duration.ofSeconds(60))
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "Erreur Google Speech-to-Text (" + response.statusCode() + ")");
            }

            String transcript = extractTranscript(response.body());
            return new SpeechTranscribeResponse(transcript, lang);
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Transcription impossible");
        }
    }

    private String extractTranscript(String responseBody) throws Exception {
        JsonNode root = objectMapper.readTree(responseBody);
        JsonNode results = root.path("results");
        if (!results.isArray() || results.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (JsonNode result : results) {
            JsonNode alternatives = result.path("alternatives");
            if (alternatives.isArray() && !alternatives.isEmpty()) {
                String part = alternatives.get(0).path("transcript").asText("").trim();
                if (!part.isEmpty()) {
                    if (!builder.isEmpty()) {
                        builder.append(' ');
                    }
                    builder.append(part);
                }
            }
        }
        return builder.toString().trim();
    }

    private String resolveEncoding(String contentType, String filename) {
        String type = contentType == null ? "" : contentType.toLowerCase(Locale.ROOT);
        String name = filename == null ? "" : filename.toLowerCase(Locale.ROOT);

        if (type.contains("wav") || name.endsWith(".wav")) {
            return "LINEAR16";
        }
        if (type.contains("webm") || name.endsWith(".webm")) {
            return "WEBM_OPUS";
        }
        if (type.contains("ogg") || name.endsWith(".ogg")) {
            return "OGG_OPUS";
        }
        if (type.contains("flac") || name.endsWith(".flac")) {
            return "FLAC";
        }
        if (type.contains("mp3") || name.endsWith(".mp3")) {
            return "MP3";
        }
        return "ENCODING_UNSPECIFIED";
    }
}
