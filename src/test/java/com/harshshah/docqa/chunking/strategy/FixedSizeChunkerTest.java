package com.harshshah.docqa.chunking.strategy;

import com.harshshah.docqa.chunking.domain.Chunk;
import com.harshshah.docqa.chunking.domain.DocumentText;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static java.util.stream.Collectors.joining;
import static org.assertj.core.api.Assertions.assertThat;

class FixedSizeChunkerTest {

    private static final Set<String> VOCAB = Set.of("lorem", "ipsum", "dolor", "sit", "amet", "consectetur");
    private final DocumentText doc = DocumentText.fromTexts(
            List.of("lorem ipsum dolor sit amet consectetur ".repeat(100).trim()));

    @Test
    void noOverlapChunksAreBoundedAndCoverTheText() {
        List<Chunk> chunks = new FixedSizeChunker(500, 0).chunk(doc);

        assertThat(chunks).hasSizeGreaterThan(1).allMatch(c -> c.length() <= 500);
        String rebuilt = chunks.stream().map(Chunk::text).collect(joining()).replaceAll("\\s+", "");
        assertThat(rebuilt).isEqualTo(doc.text().replaceAll("\\s+", ""));
    }

    @Test
    void neverCutsInsideAWord() {
        for (int overlap : new int[]{0, 100}) {
            for (Chunk c : new FixedSizeChunker(500, overlap).chunk(doc)) {
                assertThat(Arrays.stream(c.text().split("\\s+"))).allMatch(VOCAB::contains);
            }
        }
    }

    @Test
    void overlapMakesConsecutiveChunksShareText() {
        List<Chunk> chunks = new FixedSizeChunker(500, 100).chunk(doc);

        assertThat(chunks).allMatch(c -> c.length() <= 500);
        for (int i = 0; i + 1 < chunks.size(); i++) {
            assertThat(chunks.get(i + 1).startChar()).isLessThan(chunks.get(i).endChar());
        }
    }

    @Test
    void chunkTextMatchesItsOffsets() {
        for (Chunk c : new FixedSizeChunker(500, 100).chunk(doc)) {
            assertThat(c.text()).isEqualTo(doc.text().substring(c.startChar(), c.endChar()));
        }
    }
}