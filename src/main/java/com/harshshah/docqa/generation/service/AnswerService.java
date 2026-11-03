package com.harshshah.docqa.generation.service;

import com.harshshah.docqa.chunking.domain.ChunkingConfig;
import com.harshshah.docqa.chunking.domain.DocumentText;
import com.harshshah.docqa.common.InvalidRequestException;
import com.harshshah.docqa.common.LlmException;
import com.harshshah.docqa.generation.llm.LlmClient;
import com.harshshah.docqa.generation.llm.LlmResponse;
import com.harshshah.docqa.ingestion.domain.DocumentPage;
import com.harshshah.docqa.ingestion.service.DocumentQueryService;
import com.harshshah.docqa.retrieval.domain.RetrievalMode;
import com.harshshah.docqa.retrieval.service.RetrievalService;
import com.harshshah.docqa.store.domain.ScoredChunk;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnswerService {

    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final int MAX_QUESTION_CHARS = 1000;

    private final RetrievalService retrieval;
    private final DocumentQueryService documents;
    private final LlmClient llm;
    private final PromptBuilder prompts;

    public record Citation(int page, String quote, boolean verified) {}

    public record ModelCitation(int page, String quote) {}

    public record ModelAnswer(boolean answerable, String answer, List<ModelCitation> citations) {}

    public record AnswerResult(String question, boolean answerable, String answer, List<Citation> citations,
                               boolean grounded, List<String> retrievedPages, String retrievalLabel,
                               RetrievalMode mode, long retrievalMillis, String model, long generationMillis,
                               int inputTokens, int outputTokens) {}

    public AnswerResult answer(UUID documentId, ChunkingConfig config, RetrievalMode mode,
                               String question, int k) {
        if (question != null && question.length() > MAX_QUESTION_CHARS) {
            throw new InvalidRequestException("Question is longer than " + MAX_QUESTION_CHARS + " characters.");
        }
        var retrieved = retrieval.retrieve(documentId, config, mode, question, k);
        List<ScoredChunk> hits = retrieved.hits();
        if (hits.isEmpty()) {
            return new AnswerResult(question, false, PromptBuilder.ABSTAIN_TEXT, List.of(), false, List.of(),
                    retrieved.label(), mode, retrieved.millis(), "none", 0, 0, 0);
        }

        List<DocumentPage> pages = documents.getPages(documentId);
        DocumentText text = DocumentText.fromPages(pages);
        LlmResponse response = llm.generateJson(prompts.system(), prompts.user(text, hits, question),
                PromptBuilder.RESPONSE_SCHEMA);
        ModelAnswer parsed = parse(response.text());

        Map<Integer, String> pageText = pages.stream()
                .collect(Collectors.toMap(DocumentPage::getPageNumber, DocumentPage::getText));
        List<Citation> citations = parsed.citations() == null ? List.of() : parsed.citations().stream()
                .map(c -> new Citation(c.page(), c.quote(),
                        CitationVerifier.quoteOnPage(c.quote(), pageText.get(c.page()))))
                .toList();
        boolean grounded = parsed.answerable() && !citations.isEmpty()
                && citations.stream().allMatch(Citation::verified);

        return new AnswerResult(question, parsed.answerable(), parsed.answer() == null ? "" : parsed.answer(),
                citations, grounded, hits.stream().map(AnswerService::pageLabel).toList(),
                retrieved.label(), mode, retrieved.millis(), response.model(), response.millis(),
                response.inputTokens(), response.outputTokens());
    }

    private static ModelAnswer parse(String json) {
        try {
            return JSON.readValue(json, ModelAnswer.class);
        } catch (RuntimeException e) {
            String shown = json.length() > 200 ? json.substring(0, 200) + "..." : json;
            throw new LlmException("Model returned invalid JSON: " + shown, e);
        }
    }

    private static String pageLabel(ScoredChunk h) {
        return h.startPage() == h.endPage() ? String.valueOf(h.startPage()) : h.startPage() + "-" + h.endPage();
    }
}