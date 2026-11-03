package com.harshshah.docqa.retrieval.domain;

import com.harshshah.docqa.common.InvalidRequestException;

import java.util.Locale;

public enum RetrievalMode {
    DENSE, KEYWORD, HYBRID;

    public static RetrievalMode parse(String value) {
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new InvalidRequestException("Unknown mode '" + value + "'. Use dense, keyword or hybrid.");
        }
    }
}