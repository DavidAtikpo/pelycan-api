package com.pelycan.api.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Configuration
@EnableConfigurationProperties({JwtProperties.class, CorsProperties.class, S3Properties.class, CloudinaryProperties.class, AgoraProperties.class, FirebaseProperties.class, SpeechProperties.class, SmsProperties.class})
public class AppConfig {

    @Bean
    S3Client s3Client(@org.springframework.beans.factory.annotation.Value("${pelycan.aws.region:eu-west-3}") String region) {
        return S3Client.builder().region(Region.of(region)).build();
    }

    @Bean
    S3Presigner s3Presigner(@org.springframework.beans.factory.annotation.Value("${pelycan.aws.region:eu-west-3}") String region) {
        return S3Presigner.builder().region(Region.of(region)).build();
    }
}
