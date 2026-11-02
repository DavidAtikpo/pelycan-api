package com.pelycan.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class PelycanApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(PelycanApiApplication.class, args);
    }
}
