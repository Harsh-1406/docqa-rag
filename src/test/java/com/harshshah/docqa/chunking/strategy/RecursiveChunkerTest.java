package com.harshshah.docqa.chunking.strategy;

import com.harshshah.docqa.chunking.domain.Chunk;
import com.harshshah.docqa.chunking.domain.DocumentText;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RecursiveChunkerTest {

    private static String para(String word) {
        return (word + " ").repeat(16).trim();   // 95 chars
    }

    private final String p1 = para("alpha");
    private final String p2 = para("bravo");
    private final String p3 = para("delta");
    private final DocumentText doc = DocumentText.fromTexts(List.of(p1 + "\n\n" + p2 + "\n\n" + p3));

    @Test
    void prefersParagraphBoundaries() {
        List<Chunk> chunks = new RecursiveChunker(100, 0).chunk(doc);
        assertThat(chunks).extracting(Chunk::text).containsExactly(p1, p2, p3);
    }

    @Test
    void overlapRepeatsTheTrailingParagraph() {
        List<Chunk> chunks = new RecursiveChunker(200, 100).chunk(doc);

        assertThat(chunks).hasSize(2);
        assertThat(chunks.get(0).text()).contains(p2);
        assertThat(chunks.get(1).text()).contains(p2);
    }

    @Test
    void hardCutsTextWithNoSeparators() {
        DocumentText blob = DocumentText.fromTexts(List.of("x".repeat(450)));
        List<Chunk> chunks = new RecursiveChunker(100, 0).chunk(blob);

        assertThat(chunks).hasSize(5).allMatch(c -> c.length() <= 100);
    }
}