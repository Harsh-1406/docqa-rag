package com.harshshah.docqa.retrieval.strategy;

import com.harshshah.docqa.retrieval.domain.RetrievalMode;
import com.harshshah.docqa.retrieval.domain.Retriever;
import com.harshshah.docqa.store.domain.ScoredChunk;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class HybridRetriever implements Retriever {

    private static final int RRF_K = 60;   // the constant from the original RRF paper

    private final DenseRetriever dense;
    private final KeywordRetriever keyword;

    @Override
    public RetrievalMode mode() {
        return RetrievalMode.HYBRID;
    }

    @Override
    public List<ScoredChunk> retrieve(UUID chunkSetId, String query, int k) {
        int pool = Math.min(100, Math.max(k * 4, 50));   // fuse deeper candidate lists than the k we return
        return ReciprocalRankFusion.fuse(
                List.of(dense.retrieve(chunkSetId, query, pool), keyword.retrieve(chunkSetId, query, pool)),
                RRF_K, k);
    }
}