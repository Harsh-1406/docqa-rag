package com.harshshah.docqa.store.service;

import com.harshshah.docqa.chunking.domain.Chunk;
import com.harshshah.docqa.chunking.domain.ChunkingConfig;
import com.harshshah.docqa.chunking.service.ChunkingService;
import com.harshshah.docqa.common.InvalidRequestException;
import com.harshshah.docqa.embedding.Embedder;
import com.harshshah.docqa.store.domain.ChunkSet;
import com.harshshah.docqa.store.domain.ChunkSetRepository;
import com.harshshah.docqa.store.domain.ChunkVectorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

// No @Transactional on the method: embedding takes minutes and must not hold a DB connection.
// Only the final write runs inside a transaction (TransactionTemplate).
@Slf4j
@Service
@RequiredArgsConstructor
public class IndexingService {

    private final ChunkingService chunking;
    private final Embedder embedder;
    private final ChunkSetRepository chunkSets;
    private final ChunkVectorRepository vectors;
    private final TransactionTemplate tx;

    public record IndexResult(ChunkSet chunkSet, boolean alreadyIndexed, long embeddingMillis) {}

    public IndexResult index(UUID documentId, ChunkingConfig config, boolean force) {
        String label = chunking.label(config);
        Optional<ChunkSet> existing = chunkSets.findByDocumentIdAndLabel(documentId, label);
        if (existing.isPresent() && !force) {
            return new IndexResult(existing.get(), true, 0);
        }

        var result = chunking.chunk(documentId, config);
        List<Chunk> chunks = result.chunks();
        if (chunks.isEmpty()) {
            throw new InvalidRequestException("This configuration produced no chunks.");
        }

        log.info("Indexing {} chunks for {} with {}", chunks.size(), label, embedder.modelName());
        long start = System.nanoTime();
        List<float[]> vecs = embedder.embedDocuments(chunks.stream().map(Chunk::text).toList());
        long embeddingMillis = (System.nanoTime() - start) / 1_000_000;

        ChunkSet saved = tx.execute(status -> {
            existing.ifPresent(old -> {
                chunkSets.delete(old);
                chunkSets.flush();   // free the (document_id, label) unique key before re-inserting
            });
            ChunkSet set = chunkSets.saveAndFlush(new ChunkSet(documentId, label,
                    config.strategy().name().toLowerCase(), config.size(), config.overlap(),
                    embedder.modelName(), embedder.dimensions(), chunks.size()));
            vectors.insertAll(set.getId(), chunks, vecs);
            return set;
        });
        return new IndexResult(saved, false, embeddingMillis);
    }

    public List<ChunkSet> list(UUID documentId) {
        return chunkSets.findByDocumentIdOrderByCreatedAt(documentId);
    }
}