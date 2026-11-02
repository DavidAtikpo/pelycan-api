package com.pelycan.api.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pelycan.api.config.CloudinaryProperties;
import com.pelycan.api.config.S3Properties;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

@Service
public class StorageService {

    private final S3Presigner presigner;
    private final S3Client s3Client;
    private final S3Properties s3Properties;
    private final CloudinaryProperties cloudinaryProperties;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;

    public StorageService(
            S3Presigner presigner,
            S3Client s3Client,
            S3Properties s3Properties,
            CloudinaryProperties cloudinaryProperties,
            ObjectMapper objectMapper) {
        this.presigner = presigner;
        this.s3Client = s3Client;
        this.s3Properties = s3Properties;
        this.cloudinaryProperties = cloudinaryProperties;
        this.objectMapper = objectMapper;
        this.restClient = RestClient.create();
    }

    public Map<String, String> createUploadUrl(String folder, String contentType, String extension) {
        if (!isS3Configured()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Stockage S3 non configuré");
        }
        String key = buildObjectKey(folder, extension);
        PutObjectRequest objectRequest = PutObjectRequest.builder()
                .bucket(s3Properties.bucket())
                .key(key)
                .contentType(contentType)
                .build();
        var presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(s3Properties.presignDurationMinutes()))
                .putObjectRequest(objectRequest)
                .build();
        var presigned = presigner.presignPutObject(presignRequest);
        return Map.of(
                "uploadUrl", presigned.url().toString(),
                "objectKey", key,
                "publicUrl", "https://%s.s3.amazonaws.com/%s".formatted(s3Properties.bucket(), key));
    }

    public Map<String, String> uploadDirect(
            byte[] data,
            String folder,
            String contentType,
            String originalFileName) {
        if (isS3Configured()) {
            String extension = extractExtension(originalFileName, contentType);
            String key = buildObjectKey(folder, extension);
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(s3Properties.bucket())
                    .key(key)
                    .contentType(contentType)
                    .build();
            s3Client.putObject(request, RequestBody.fromBytes(data));
            return Map.of(
                    "publicUrl", "https://%s.s3.amazonaws.com/%s".formatted(s3Properties.bucket(), key),
                    "objectKey", key);
        }
        if (cloudinaryProperties.isConfigured()) {
            return Map.of("publicUrl", uploadToCloudinary(data, folder, contentType, originalFileName));
        }
        throw new ResponseStatusException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "Aucun stockage configuré (S3 ou Cloudinary)");
    }

    private String uploadToCloudinary(
            byte[] data,
            String folder,
            String contentType,
            String originalFileName) {
        long timestamp = Instant.now().getEpochSecond();
        String folderName = folder == null || folder.isBlank() ? "uploads" : folder.trim();

        TreeMap<String, String> signParams = new TreeMap<>();
        signParams.put("folder", folderName);
        signParams.put("timestamp", String.valueOf(timestamp));
        String signature = cloudinarySignature(signParams);

        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        builder.part("file", new ByteArrayResource(data) {
            @Override
            public String getFilename() {
                return originalFileName == null || originalFileName.isBlank()
                        ? "upload.bin"
                        : originalFileName;
            }
        }).contentType(MediaType.parseMediaType(
                contentType == null || contentType.isBlank() ? "application/octet-stream" : contentType));
        builder.part("api_key", cloudinaryProperties.apiKey());
        builder.part("timestamp", String.valueOf(timestamp));
        builder.part("signature", signature);
        builder.part("folder", folderName);

        String responseBody = restClient.post()
                .uri("https://api.cloudinary.com/v1_1/%s/auto/upload"
                        .formatted(cloudinaryProperties.cloudName()))
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(builder.build())
                .retrieve()
                .body(String.class);

        try {
            JsonNode json = objectMapper.readTree(responseBody);
            JsonNode secureUrl = json.get("secure_url");
            if (secureUrl != null && !secureUrl.asText().isBlank()) {
                return secureUrl.asText();
            }
            JsonNode url = json.get("url");
            if (url != null && !url.asText().isBlank()) {
                return url.asText();
            }
            JsonNode error = json.get("error");
            String message = error != null ? error.asText() : "Réponse Cloudinary invalide";
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, message);
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Échec de l'upload Cloudinary",
                    ex);
        }
    }

    private String cloudinarySignature(TreeMap<String, String> params) {
        StringBuilder payload = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (!first) {
                payload.append('&');
            }
            payload.append(entry.getKey()).append('=').append(entry.getValue());
            first = false;
        }
        payload.append(cloudinaryProperties.apiSecret());
        return sha1Hex(payload.toString());
    }

    private static String sha1Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-1 indisponible", ex);
        }
    }

    private boolean isS3Configured() {
        return s3Properties.bucket() != null && !s3Properties.bucket().isBlank();
    }

    private static String buildObjectKey(String folder, String extension) {
        return "%s/%s.%s".formatted(
                folder == null || folder.isBlank() ? "uploads" : folder,
                UUID.randomUUID(),
                extension == null || extension.isBlank() ? "bin" : extension);
    }

    private static String extractExtension(String fileName, String contentType) {
        if (fileName != null && fileName.contains(".")) {
            return fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
        }
        if (contentType != null && contentType.contains("/")) {
            return contentType.substring(contentType.indexOf('/') + 1);
        }
        return "bin";
    }
}
