package com.harshshah.docqa.retrieval.strategy;

import com.harshshah.docqa.store.domain.ScoredChunk;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ReciprocalRankFusionTest {

    private static ScoredChunk c(int index) {
        return new ScoredChunk(index, 1, 1, 0, 10, "chunk " + index, 0.5);
    }

    @Test
    void chunksFoundByBothRankingsBeatChunksFoundByOne() {
        var dense = List.of(c(1), c(2), c(3));
        var keyword = List.of(c(3), c(1), c(4));

        var fused = ReciprocalRankFusion.fuse(List.of(dense, keyword), 60, 10);

        assertThat(fused).extracting(ScoredChunk::chunkIndex).containsExactly(1, 3, 2, 4);
        assertThat(fused.get(0).score()).isEqualTo(1.0 / 61 + 1.0 / 62);
    }

    @Test
    void respectsTheLimitAndHandlesEmptyRankings() {
        assertThat(ReciprocalRankFusion.fuse(List.of(List.of(c(1), c(2), c(3)), List.of()), 60, 2)).hasSize(2);
        assertThat(ReciprocalRankFusion.fuse(List.of(List.of(), List.of()), 60, 5)).isEmpty();
    }
}