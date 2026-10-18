package com.harshshah.docqa.ingestion.api;

import java.util.UUID;

public record PageResponse(UUID documentId, int pageNumber, int charCount, String text) {}