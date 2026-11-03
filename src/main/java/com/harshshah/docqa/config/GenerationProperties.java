package com.harshshah.docqa.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "docqa.generation")
public record GenerationProperties(
        @DefaultValue("gemini-3.5-flash-lite") String model,
        @DefaultValue("0.0") double temperature,
        @DefaultValue("1024") int maxOutputTokens,
        @DefaultValue("5") int maxRetries,
        @DefaultValue("10") int maxRequestsPerMinute,
        @DefaultValue("100000") int maxTokensPerMinute) {}