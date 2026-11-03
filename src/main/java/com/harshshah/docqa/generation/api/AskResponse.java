package com.harshshah.docqa.generation.api;

import com.harshshah.docqa.generation.service.AnswerService.AnswerResult;

import java.util.List;

public record AskResponse(String question, boolean answerable, String answer, boolean grounded,
                          List<CitationView> citations, List<String> retrievedPages, Meta meta) {

    public record CitationView(int page, String quote, boolean verified) {}

    public record Meta(String retrievalLabel, String mode, long retrievalMillis, String model,
                       long generationMillis, int inputTokens, int outputTokens) {}

    static AskResponse from(AnswerResult r) {
        return new AskResponse(r.question(), r.answerable(), r.answer(), r.grounded(),
                r.citations().stream().map(c -> new CitationView(c.page(), c.quote(), c.verified())).toList(),
                r.retrievedPages(),
                new Meta(r.retrievalLabel(), r.mode().name().toLowerCase(), r.retrievalMillis(), r.model(),
                        r.generationMillis(), r.inputTokens(), r.outputTokens()));
    }
}