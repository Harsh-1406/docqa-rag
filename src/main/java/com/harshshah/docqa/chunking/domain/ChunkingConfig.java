package com.harshshah.docqa.chunking.domain;

import com.harshshah.docqa.common.InvalidRequestException;

/** size = max chunk size in characters. overlap is ignored by the semantic strategy. */
public record ChunkingConfig(ChunkingStrategy strategy, int size, int overlap) {

    public ChunkingConfig {
        if (size < 100) {
            throw new InvalidRequestException("size must be at least 100 characters.");
        }
        if (overlap < 0 || overlap >= size) {
            throw new InvalidRequestException("overlap must be between 0 and size - 1.");
        }
    }
}