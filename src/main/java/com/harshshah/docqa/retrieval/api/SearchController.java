package com.harshshah.docqa.retrieval.api;

import com.harshshah.docqa.chunking.service.ChunkingService;
import com.harshshah.docqa.retrieval.domain.RetrievalMode;
import com.harshshah.docqa.retrieval.service.RetrievalService;
import com.harshshah.docqa.store.domain.ScoredChunk;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

@RestController
@RequestMapping("/api/documents/{id}/search")
@RequiredArgsConstructor
public class SearchController {

    private final RetrievalService retrieval;
    private final ChunkingService chunking;

    @GetMapping
    public SearchResponse search(@PathVariable UUID id,
                                 @RequestParam("q") String query,
                                 @RequestParam(defaultValue = "dense") String mode,
                                 @RequestParam(required = false) String strategy,
                                 @RequestParam(required = false) Integer size,
                                 @RequestParam(required = false) Integer overlap,
                                 @RequestParam(defaultValue = "5") int k) {
        var result = retrieval.retrieve(id, chunking.resolve(strategy, size, overlap),
                RetrievalMode.parse(mode), query, k);
        List<ScoredChunk> hits = result.hits();
        List<SearchResponse.Hit> view = IntStream.range(0, hits.size()).mapToObj(i -> {
            ScoredChunk h = hits.get(i);
            return new SearchResponse.Hit(i + 1, h.score(), h.startPage(), h.endPage(),
                    h.startChar(), h.endChar(), h.content());
        }).toList();
        return new SearchResponse(result.label(), result.mode().name().toLowerCase(), result.millis(), view);
    }
}