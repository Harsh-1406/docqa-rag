package com.harshshah.docqa.retrieval.strategy;

import com.harshshah.docqa.store.domain.ScoredChunk;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** score(chunk) = sum over rankings of 1 / (rrfK + rank), rank starting at 1. Needs ranks only, not comparable scores. */
public final class ReciprocalRankFusion {

    private ReciprocalRankFusion() {}

    public static List<ScoredChunk> fuse(List<List<ScoredChunk>> rankings, int rrfK, int limit) {
        Map<Integer, Double> scores = new HashMap<>();
        Map<Integer, ScoredChunk> firstSeen = new HashMap<>();
        for (List<ScoredChunk> ranking : rankings) {
            for (int i = 0; i < ranking.size(); i++) {
                ScoredChunk c = ranking.get(i);
                scores.merge(c.chunkIndex(), 1.0 / (rrfK + i + 1), Double::sum);
                firstSeen.putIfAbsent(c.chunkIndex(), c);
            }
        }
        return scores.entrySet().stream()
                .sorted((a, b) -> {
                    int byScore = Double.compare(b.getValue(), a.getValue());
                    return byScore != 0 ? byScore : Integer.compare(a.getKey(), b.getKey());
                })
                .limit(limit)
                .map(e -> {
                    ScoredChunk c = firstSeen.get(e.getKey());
                    return new ScoredChunk(c.chunkIndex(), c.startPage(), c.endPage(), c.startChar(),
                            c.endChar(), c.content(), e.getValue());
                })
                .toList();
    }
}