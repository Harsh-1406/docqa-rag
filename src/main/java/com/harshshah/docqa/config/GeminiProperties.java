package com.harshshah.docqa.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "docqa.gemini")
public record GeminiProperties(
        @DefaultValue("") String apiKey,
        @DefaultValue("gemini-embedding-001") String model,
        @DefaultValue("768") int dimensions,
        @DefaultValue("https://generativelanguage.googleapis.com/v1beta") String baseUrl,
        @DefaultValue("100") int maxBatchSize,
        @DefaultValue("80") int maxRequestsPerMinute,
        @DefaultValue("24000") int maxTokensPerMinute,
        @DefaultValue("5") int maxRetries) {}