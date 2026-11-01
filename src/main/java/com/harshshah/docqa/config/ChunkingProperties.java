package com.harshshah.docqa.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "docqa.chunking")
public record ChunkingProperties(
        @DefaultValue("recursive") String strategy,
        @DefaultValue("2000") int size,
        @DefaultValue("200") int overlap) {}