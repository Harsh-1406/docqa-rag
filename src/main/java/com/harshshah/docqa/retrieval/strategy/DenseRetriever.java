package com.harshshah.docqa.retrieval.strategy;

import com.harshshah.docqa.embedding.Embedder;
import com.harshshah.docqa.retrieval.domain.RetrievalMode;
import com.harshshah.docqa.retrieval.domain.Retriever;
import com.harshshah.docqa.store.domain.ChunkVectorRepository;
import com.harshshah.docqa.store.domain.ScoredChunk;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class DenseRetriever implements Retriever {

    private static final int CACHE_SIZE = 1000;

    private final Embedder embedder;
    private final ChunkVectorRepository vectors;

    // Every query embedding costs one request of the daily quota, so identical queries are embedded once.
    private final Map<String, float[]> queryCache = Collections.synchronizedMap(
            new LinkedHashMap<>(256, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, float[]> eldest) {
                    return size() > CACHE_SIZE;
                }
            });

    @Override
    public RetrievalMode mode() {
        return RetrievalMode.DENSE;
    }

    @Override
    public List<ScoredChunk> retrieve(UUID chunkSetId, String query, int k) {
        float[] vector = queryCache.computeIfAbsent(embedder.modelName() + "|" + query,
                key -> embedder.embedQuery(query));
        return vectors.search(chunkSetId, vector, k);
    }
}