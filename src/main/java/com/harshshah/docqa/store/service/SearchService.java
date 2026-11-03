package com.harshshah.docqa.store.service;

import com.harshshah.docqa.chunking.domain.ChunkingConfig;
import com.harshshah.docqa.chunking.service.ChunkingService;
import com.harshshah.docqa.common.InvalidRequestException;
import com.harshshah.docqa.common.NotFoundException;
import com.harshshah.docqa.embedding.Embedder;
import com.harshshah.docqa.store.domain.ChunkSet;
import com.harshshah.docqa.store.domain.ChunkSetRepository;
import com.harshshah.docqa.store.domain.ChunkVectorRepository;
import com.harshshah.docqa.store.domain.ScoredChunk;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SearchService {

    private final ChunkingService chunking;
    private final Embedder embedder;
    private final ChunkSetRepository chunkSets;
    private final ChunkVectorRepository vectors;

    public record SearchResult(String label, long embedMillis, long searchMillis, List<ScoredChunk> hits) {}

    public SearchResult search(UUID documentId, ChunkingConfig config, String query, int k) {
        if (query == null || query.isBlank()) {
            throw new InvalidRequestException("Query must not be blank.");
        }
        if (k < 1 || k > 50) {
            throw new InvalidRequestException("k must be between 1 and 50.");
        }
        String label = chunking.label(config);
        ChunkSet set = chunkSets.findByDocumentIdAndLabel(documentId, label)
                .orElseThrow(() -> new NotFoundException(
                        "No index for '" + label + "'. Call POST /api/documents/{id}/index first."));

        long t0 = System.nanoTime();
        float[] queryVector = embedder.embedQuery(query);
        long t1 = System.nanoTime();
        List<ScoredChunk> hits = vectors.search(set.getId(), queryVector, k);
        long t2 = System.nanoTime();
        return new SearchResult(label, (t1 - t0) / 1_000_000, (t2 - t1) / 1_000_000, hits);
    }
}