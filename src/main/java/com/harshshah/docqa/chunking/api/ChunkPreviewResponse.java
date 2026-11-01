package com.harshshah.docqa.chunking.api;

import com.harshshah.docqa.chunking.domain.Chunk;
import com.harshshah.docqa.chunking.service.ChunkingService.ChunkingResult;

import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

public record ChunkPreviewResponse(String strategy, long chunkingMillis, int chunkCount,
                                   LengthStats lengthStats, List<ChunkView> chunks) {

    public record LengthStats(int min, int median, int mean, int max) {}

    public record ChunkView(int index, int startPage, int endPage, int startChar, int endChar,
                            int length, String text) {}

    static ChunkPreviewResponse from(ChunkingResult result, int from, int limit) {
        List<Chunk> all = result.chunks();
        int[] lens = all.stream().mapToInt(Chunk::length).sorted().toArray();
        LengthStats stats = lens.length == 0
                ? new LengthStats(0, 0, 0, 0)
                : new LengthStats(lens[0], lens[lens.length / 2],
                (int) Arrays.stream(lens).average().orElse(0), lens[lens.length - 1]);

        int begin = Math.max(from, 0);
        List<ChunkView> view = IntStream.range(begin, Math.min(all.size(), begin + Math.max(limit, 0)))
                .mapToObj(i -> {
                    Chunk c = all.get(i);
                    return new ChunkView(i, c.startPage(), c.endPage(), c.startChar(), c.endChar(),
                            c.length(), c.text());
                }).toList();

        return new ChunkPreviewResponse(result.label(), result.millis(), all.size(), stats, view);
    }
}