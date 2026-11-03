package com.harshshah.docqa.store.api;

import com.harshshah.docqa.chunking.service.ChunkingService;
import com.harshshah.docqa.store.domain.ChunkSet;
import com.harshshah.docqa.store.domain.ScoredChunk;
import com.harshshah.docqa.store.service.IndexingService;
import com.harshshah.docqa.store.service.SearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

@RestController
@RequestMapping("/api/documents/{id}")
@RequiredArgsConstructor
public class IndexController {

    private final IndexingService indexing;
    private final SearchService search;
    private final ChunkingService chunking;

    public record IndexResponse(String label, int chunkCount, String embeddingModel, int dimensions,
                                boolean alreadyIndexed, long embeddingMillis) {
        static IndexResponse from(ChunkSet s, boolean already, long millis) {
            return new IndexResponse(s.getLabel(), s.getChunkCount(), s.getEmbeddingModel(),
                    s.getDimensions(), already, millis);
        }
    }

    public record Hit(int rank, double score, int startPage, int endPage, int startChar, int endChar, String text) {}

    public record SearchResponse(String label, long embedMillis, long searchMillis, List<Hit> hits) {}

    @PostMapping("/index")
    public ResponseEntity<IndexResponse> index(@PathVariable UUID id,
                                               @RequestParam(required = false) String strategy,
                                               @RequestParam(required = false) Integer size,
                                               @RequestParam(required = false) Integer overlap,
                                               @RequestParam(defaultValue = "false") boolean force) {
        var result = indexing.index(id, chunking.resolve(strategy, size, overlap), force);
        var body = IndexResponse.from(result.chunkSet(), result.alreadyIndexed(), result.embeddingMillis());
        return ResponseEntity.status(result.alreadyIndexed() ? HttpStatus.OK : HttpStatus.CREATED).body(body);
    }

    @GetMapping("/index")
    public List<IndexResponse> listIndexes(@PathVariable UUID id) {
        return indexing.list(id).stream().map(s -> IndexResponse.from(s, true, 0)).toList();
    }

    @GetMapping("/search")
    public SearchResponse search(@PathVariable UUID id,
                                 @RequestParam("q") String query,
                                 @RequestParam(required = false) String strategy,
                                 @RequestParam(required = false) Integer size,
                                 @RequestParam(required = false) Integer overlap,
                                 @RequestParam(defaultValue = "5") int k) {
        var result = search.search(id, chunking.resolve(strategy, size, overlap), query, k);
        List<ScoredChunk> hits = result.hits();
        List<Hit> view = IntStream.range(0, hits.size()).mapToObj(i -> {
            ScoredChunk h = hits.get(i);
            return new Hit(i + 1, h.score(), h.startPage(), h.endPage(), h.startChar(), h.endChar(), h.content());
        }).toList();
        return new SearchResponse(result.label(), result.embedMillis(), result.searchMillis(), view);
    }
}