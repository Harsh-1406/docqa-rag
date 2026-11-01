package com.harshshah.docqa.chunking.api;

import com.harshshah.docqa.chunking.service.ChunkingService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/documents/{id}/chunks")
@RequiredArgsConstructor
public class ChunkPreviewController {

    private final ChunkingService chunking;

    @GetMapping("/preview")
    public ChunkPreviewResponse preview(@PathVariable UUID id,
                                        @RequestParam(required = false) String strategy,
                                        @RequestParam(required = false) Integer size,
                                        @RequestParam(required = false) Integer overlap,
                                        @RequestParam(defaultValue = "0") int from,
                                        @RequestParam(defaultValue = "5") int limit) {
        var config = chunking.resolve(strategy, size, overlap);
        var result = chunking.chunk(id, config);
        return ChunkPreviewResponse.from(result, from, Math.min(limit, 50));
    }
}