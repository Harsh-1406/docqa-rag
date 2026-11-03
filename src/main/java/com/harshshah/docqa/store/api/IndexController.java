package com.harshshah.docqa.store.api;

import com.harshshah.docqa.chunking.service.ChunkingService;
import com.harshshah.docqa.store.domain.ChunkSet;
import com.harshshah.docqa.store.service.IndexingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/documents/{id}")
@RequiredArgsConstructor
public class IndexController {

    private final IndexingService indexing;
    private final ChunkingService chunking;

    public record IndexResponse(String label, int chunkCount, String embeddingModel, int dimensions,
                                boolean alreadyIndexed, long embeddingMillis) {
        static IndexResponse from(ChunkSet s, boolean already, long millis) {
            return new IndexResponse(s.getLabel(), s.getChunkCount(), s.getEmbeddingModel(),
                    s.getDimensions(), already, millis);
        }
    }

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
}