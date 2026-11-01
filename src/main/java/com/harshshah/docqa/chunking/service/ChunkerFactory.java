package com.harshshah.docqa.chunking.service;

import com.harshshah.docqa.chunking.domain.ChunkingConfig;
import com.harshshah.docqa.chunking.strategy.*;
import com.harshshah.docqa.common.InvalidRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ChunkerFactory {

    private final ObjectProvider<TextEmbedder> embedder;

    public Chunker create(ChunkingConfig c) {
        return switch (c.strategy()) {
            case FIXED -> new FixedSizeChunker(c.size(), c.overlap());
            case RECURSIVE -> new RecursiveChunker(c.size(), c.overlap());
            case SEMANTIC -> new SemanticChunker(
                    embedder.getIfAvailable(() -> {
                        throw new InvalidRequestException(
                                "Semantic chunking needs an embedding model, which is added in Module 3.");
                    }),
                    c.size(), c.size() / 4, 1, 90.0);
        };
    }
}