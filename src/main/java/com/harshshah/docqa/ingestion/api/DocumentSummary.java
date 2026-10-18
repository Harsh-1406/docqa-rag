package com.harshshah.docqa.ingestion.api;

import com.harshshah.docqa.ingestion.domain.Document;
import java.time.Instant;
import java.util.UUID;

public record DocumentSummary(UUID id, String filename, int pageCount, long charCount, Instant uploadedAt) {
    static DocumentSummary from(Document d) {
        return new DocumentSummary(d.getId(), d.getFilename(), d.getPageCount(),
                d.getCharCount(), d.getUploadedAt());
    }
}