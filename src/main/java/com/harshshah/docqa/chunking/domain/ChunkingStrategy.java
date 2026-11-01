package com.harshshah.docqa.chunking.domain;

import com.harshshah.docqa.common.InvalidRequestException;

import java.util.Locale;

public enum ChunkingStrategy {
    FIXED, RECURSIVE, SEMANTIC;

    public static ChunkingStrategy parse(String value) {
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new InvalidRequestException(
                    "Unknown chunking strategy '" + value + "'. Use fixed, recursive or semantic.");
        }
    }
}