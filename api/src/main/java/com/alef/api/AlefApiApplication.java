package com.alef.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the ALEF API service.
 *
 * Runs in two Spring profiles from one codebase:
 * - "api" profile: web API, concierge, RAG (feature 001+)
 * - "worker" profile: Redis Streams consumer (feature 001+)
 *
 * Feature 003 (Portfolio with Filtering) is the first concrete read path.
 */
@SpringBootApplication
public class AlefApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(AlefApiApplication.class, args);
    }
}
