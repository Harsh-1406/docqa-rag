package com.harshshah.docqa.retrieval.strategy;

import com.harshshah.docqa.retrieval.domain.RetrievalMode;
import com.harshshah.docqa.retrieval.domain.Retriever;
import com.harshshah.docqa.store.domain.ChunkTextRepository;
import com.harshshah.docqa.store.domain.ScoredChunk;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class KeywordRetriever implements Retriever {

    private final ChunkTextRepository texts;

    @Override
    public RetrievalMode mode() {
        return RetrievalMode.KEYWORD;
    }

    @Override
    public List<ScoredChunk> retrieve(UUID chunkSetId, String query, int k) {
        String tsQuery = KeywordQuery.toTsQuery(query);
        return tsQuery.isEmpty() ? List.of() : texts.search(chunkSetId, tsQuery, k);
    }
}