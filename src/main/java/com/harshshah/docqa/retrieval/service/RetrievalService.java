package com.harshshah.docqa.retrieval.service;

import com.harshshah.docqa.chunking.domain.ChunkingConfig;
import com.harshshah.docqa.chunking.service.ChunkingService;
import com.harshshah.docqa.common.InvalidRequestException;
import com.harshshah.docqa.retrieval.domain.RetrievalMode;
import com.harshshah.docqa.retrieval.domain.Retriever;
import com.harshshah.docqa.store.domain.ChunkSet;
import com.harshshah.docqa.store.domain.ScoredChunk;
import com.harshshah.docqa.store.service.ChunkSetService;
import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class RetrievalService {

    private final ChunkingService chunking;
    private final ChunkSetService chunkSets;
    private final Map<RetrievalMode, Retriever> retrievers = new EnumMap<>(RetrievalMode.class);

    public RetrievalService(ChunkingService chunking, ChunkSetService chunkSets, List<Retriever> all) {
        this.chunking = chunking;
        this.chunkSets = chunkSets;
        all.forEach(r -> retrievers.put(r.mode(), r));
    }

    public record RetrievalResult(String label, RetrievalMode mode, long millis, List<ScoredChunk> hits) {}

    public RetrievalResult retrieve(UUID documentId, ChunkingConfig config, RetrievalMode mode,
                                    String query, int k) {
        if (query == null || query.isBlank()) {
            throw new InvalidRequestException("Query must not be blank.");
        }
        if (k < 1 || k > 50) {
            throw new InvalidRequestException("k must be between 1 and 50.");
        }
        String label = chunking.label(config);
        ChunkSet set = chunkSets.require(documentId, label);

        long start = System.nanoTime();
        List<ScoredChunk> hits = retrievers.get(mode).retrieve(set.getId(), query, k);
        return new RetrievalResult(label, mode, (System.nanoTime() - start) / 1_000_000, hits);
    }
}