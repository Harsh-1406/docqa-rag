package com.harshshah.docqa.chunking.strategy;

import com.harshshah.docqa.chunking.domain.Chunk;
import com.harshshah.docqa.chunking.domain.DocumentText;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.joining;
import static org.assertj.core.api.Assertions.assertThat;

class SemanticChunkerTest {

    private static final String TOPIC_A =
            "Revenue grew strongly this year. Revenue from services increased. "
                    + "Finance teams reported record revenue. Revenue targets were exceeded. Revenue outlook remains strong.";
    private static final String TOPIC_B =
            "The dog chased the ball. My pet dog loves walks. A pet needs a daily walk. "
                    + "The dog sleeps all day. Every pet dog is loyal.";

    /** Two orthogonal "topics" so the test needs no real model. */
    private final TextEmbedder topicEmbedder = texts -> texts.stream().map(t -> {
        String s = t.toLowerCase();
        return new float[]{s.contains("revenue") ? 1f : 0f, (s.contains("dog") || s.contains("pet")) ? 1f : 0f};
    }).toList();

    @Test
    void splitsWhereTheTopicChanges() {
        DocumentText doc = DocumentText.fromTexts(List.of(TOPIC_A + " " + TOPIC_B));

        List<Chunk> chunks = new SemanticChunker(topicEmbedder, 5000, 20, 0, 80).chunk(doc);

        assertThat(chunks).hasSize(2);
        assertThat(chunks.get(0).text()).contains("Revenue").doesNotContain("dog");
        assertThat(chunks.get(1).text()).contains("dog").doesNotContain("Revenue");
    }

    @Test
    void maxSizeIsEnforcedEvenWithoutTopicChange() {
        String text = IntStream.range(0, 40).mapToObj(i -> "Sentence number " + i + " is here.").collect(joining(" "));
        TextEmbedder constant = texts -> texts.stream().map(t -> new float[]{1f, 0f}).toList();

        List<Chunk> chunks = new SemanticChunker(constant, 100, 20, 0, 90)
                .chunk(DocumentText.fromTexts(List.of(text)));

        assertThat(chunks).hasSizeGreaterThan(1).allMatch(c -> c.length() <= 100);
    }
}