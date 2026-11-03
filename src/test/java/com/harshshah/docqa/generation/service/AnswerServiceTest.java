package com.harshshah.docqa.generation.service;

import com.harshshah.docqa.chunking.domain.ChunkingConfig;
import com.harshshah.docqa.chunking.domain.ChunkingStrategy;
import com.harshshah.docqa.chunking.domain.DocumentText;
import com.harshshah.docqa.common.LlmException;
import com.harshshah.docqa.generation.llm.LlmClient;
import com.harshshah.docqa.generation.llm.LlmResponse;
import com.harshshah.docqa.ingestion.domain.DocumentPage;
import com.harshshah.docqa.ingestion.service.DocumentQueryService;
import com.harshshah.docqa.retrieval.domain.RetrievalMode;
import com.harshshah.docqa.retrieval.service.RetrievalService;
import com.harshshah.docqa.retrieval.service.RetrievalService.RetrievalResult;
import com.harshshah.docqa.store.domain.ScoredChunk;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnswerServiceTest {

    @Mock RetrievalService retrieval;
    @Mock DocumentQueryService documents;
    @Mock LlmClient llm;

    private final UUID doc = UUID.randomUUID();
    private final ChunkingConfig config = new ChunkingConfig(ChunkingStrategy.RECURSIVE, 2000, 200);
    private final List<DocumentPage> pages = List.of(
            new DocumentPage(doc, 1, "Revenue was 100 dollars in 2025."),
            new DocumentPage(doc, 2, "Costs were 40 dollars."));
    private AnswerService service;

    @BeforeEach
    void setUp() {
        service = new AnswerService(retrieval, documents, llm, new PromptBuilder());
    }

    private void stubRetrieval(List<ScoredChunk> hits) {
        when(retrieval.retrieve(any(), any(), any(), anyString(), anyInt()))
                .thenReturn(new RetrievalResult("label", RetrievalMode.HYBRID, 3, hits));
    }

    private void stubModel(String json) {
        int end = DocumentText.fromPages(pages).text().length();
        stubRetrieval(List.of(new ScoredChunk(0, 1, 2, 0, end, "x", 0.9)));
        when(documents.getPages(doc)).thenReturn(pages);
        when(llm.generateJson(anyString(), anyString(), anyMap()))
                .thenReturn(new LlmResponse(json, "model-x", 100, 20, 50));
    }

    private AnswerService.AnswerResult ask() {
        return service.answer(doc, config, RetrievalMode.HYBRID, "How much revenue?", 5);
    }

    @Test
    void noRetrievalHitsAbstainsWithoutCallingTheModel() {
        stubRetrieval(List.of());

        var result = ask();

        assertThat(result.answerable()).isFalse();
        assertThat(result.answer()).isEqualTo(PromptBuilder.ABSTAIN_TEXT);
        verifyNoInteractions(llm, documents);
    }

    @Test
    void verifiedCitationMakesTheAnswerGrounded() {
        stubModel("""
                {"answerable": true, "answer": "100 dollars.",
                 "citations": [{"page": 1, "quote": "Revenue was 100 dollars"}]}
                """);

        var result = ask();

        assertThat(result.grounded()).isTrue();
        assertThat(result.citations()).singleElement().matches(c -> c.verified());
        assertThat(result.model()).isEqualTo("model-x");
    }

    @Test
    void fabricatedQuoteIsNotVerified() {
        stubModel("""
                {"answerable": true, "answer": "500 dollars.",
                 "citations": [{"page": 1, "quote": "Revenue was 500 dollars"}]}
                """);

        var result = ask();

        assertThat(result.grounded()).isFalse();
        assertThat(result.citations()).singleElement().matches(c -> !c.verified());
    }

    @Test
    void quoteCitedOnTheWrongPageIsNotVerified() {
        stubModel("""
                {"answerable": true, "answer": "100 dollars.",
                 "citations": [{"page": 2, "quote": "Revenue was 100 dollars"}]}
                """);

        assertThat(ask().grounded()).isFalse();
    }

    @Test
    void invalidModelJsonBecomesAnLlmException() {
        stubModel("not json at all");

        assertThatThrownBy(this::ask).isInstanceOf(LlmException.class);
    }
}