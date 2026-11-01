package com.harshshah.docqa.chunking.service;

import com.harshshah.docqa.chunking.domain.*;
import com.harshshah.docqa.chunking.strategy.Chunker;
import com.harshshah.docqa.config.ChunkingProperties;
import com.harshshah.docqa.ingestion.service.DocumentQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

// No @Transactional here on purpose: semantic chunking makes slow embedding calls,
// and we must not hold a DB connection while waiting for them.
@Service
@RequiredArgsConstructor
public class ChunkingService {

    private final DocumentQueryService documentQueries;
    private final ChunkerFactory factory;
    private final ChunkingProperties defaults;

    public record ChunkingResult(String label, List<Chunk> chunks, long millis) {}

    public ChunkingConfig resolve(String strategy, Integer size, Integer overlap) {
        return new ChunkingConfig(
                ChunkingStrategy.parse(strategy != null ? strategy : defaults.strategy()),
                size != null ? size : defaults.size(),
                overlap != null ? overlap : defaults.overlap());
    }

    public ChunkingResult chunk(UUID documentId, ChunkingConfig config) {
        DocumentText text = DocumentText.fromPages(documentQueries.getPages(documentId));
        Chunker chunker = factory.create(config);
        long start = System.nanoTime();
        List<Chunk> chunks = chunker.chunk(text);
        return new ChunkingResult(chunker.name(), chunks, (System.nanoTime() - start) / 1_000_000);
    }
}