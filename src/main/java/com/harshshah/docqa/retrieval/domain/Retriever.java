package com.harshshah.docqa.retrieval.domain;

import com.harshshah.docqa.store.domain.ScoredChunk;

import java.util.List;
import java.util.UUID;

/** Scores are only comparable within one retriever (cosine, ts_rank_cd and RRF are on different scales). */
public interface Retriever {
    RetrievalMode mode();

    List<ScoredChunk> retrieve(UUID chunkSetId, String query, int k);
}