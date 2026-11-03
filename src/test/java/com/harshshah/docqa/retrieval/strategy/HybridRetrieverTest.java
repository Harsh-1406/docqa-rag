package com.harshshah.docqa.retrieval.strategy;

import com.harshshah.docqa.store.domain.ScoredChunk;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HybridRetrieverTest {

    @Mock DenseRetriever dense;
    @Mock KeywordRetriever keyword;
    @InjectMocks HybridRetriever hybrid;

    private static ScoredChunk c(int index) {
        return new ScoredChunk(index, 1, 1, 0, 10, "chunk " + index, 0.5);
    }

    @Test
    void fusesBothRetrieversOverADeeperPool() {
        UUID set = UUID.randomUUID();
        when(dense.retrieve(set, "q", 50)).thenReturn(List.of(c(1), c(2)));
        when(keyword.retrieve(set, "q", 50)).thenReturn(List.of(c(2), c(3)));

        var hits = hybrid.retrieve(set, "q", 2);

        assertThat(hits).extracting(ScoredChunk::chunkIndex).containsExactly(2, 1);
    }
}