package com.harshshah.docqa.generation.api;

import com.harshshah.docqa.chunking.service.ChunkingService;
import com.harshshah.docqa.generation.service.AnswerService;
import com.harshshah.docqa.retrieval.domain.RetrievalMode;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/documents/{id}/ask")
@RequiredArgsConstructor
public class AskController {

    private final AnswerService answers;
    private final ChunkingService chunking;

    @GetMapping
    public AskResponse ask(@PathVariable UUID id,
                           @RequestParam("q") String question,
                           @RequestParam(defaultValue = "hybrid") String mode,
                           @RequestParam(required = false) String strategy,
                           @RequestParam(required = false) Integer size,
                           @RequestParam(required = false) Integer overlap,
                           @RequestParam(defaultValue = "5") int k) {
        var result = answers.answer(id, chunking.resolve(strategy, size, overlap),
                RetrievalMode.parse(mode), question, k);
        return AskResponse.from(result);
    }
}