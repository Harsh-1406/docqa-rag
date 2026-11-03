package com.harshshah.docqa.generation.service;

import com.harshshah.docqa.chunking.domain.DocumentText;
import com.harshshah.docqa.store.domain.ScoredChunk;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PromptBuilderTest {

    @Test
    void labelsEachPageSegmentAndIncludesTheQuestion() {
        DocumentText doc = DocumentText.fromTexts(List.of("alpha page one", "beta page two"));
        ScoredChunk hit = new ScoredChunk(0, 1, 2, 0, doc.text().length(), doc.text(), 0.9);

        String prompt = new PromptBuilder().user(doc, List.of(hit), "  What is beta?  ");

        assertThat(prompt).contains("[Page 1]", "alpha page one", "[Page 2]", "beta page two", "What is beta?");
    }

    @Test
    void labelsExcerptsWithLettersNotNumbers() {
        DocumentText doc = DocumentText.fromTexts(List.of("alpha page one", "beta page two"));
        ScoredChunk first = new ScoredChunk(0, 1, 1, 0, 14, "alpha page one", 0.9);
        ScoredChunk second = new ScoredChunk(1, 2, 2, 16, doc.text().length(), "beta page two", 0.8);

        String prompt = new PromptBuilder().user(doc, List.of(first, second), "Q?");

        assertThat(prompt).contains("=== Excerpt A ===", "=== Excerpt B ===");
        assertThat(prompt).doesNotContain("Excerpt 1", "Excerpt 2");
    }

    @Test
    void systemPromptTellsTheModelWhereCitationPagesComeFrom() {
        assertThat(new PromptBuilder().system()).contains("[Page N] marker", "Excerpt letters are not page numbers");
    }
}