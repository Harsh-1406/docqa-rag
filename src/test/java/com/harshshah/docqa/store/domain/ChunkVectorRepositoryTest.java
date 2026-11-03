package com.harshshah.docqa.store.domain;

import com.harshshah.docqa.TestcontainersConfiguration;
import com.harshshah.docqa.chunking.domain.Chunk;
import com.harshshah.docqa.ingestion.domain.Document;
import com.harshshah.docqa.ingestion.domain.DocumentRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class ChunkVectorRepositoryTest {

    @Autowired ChunkVectorRepository vectors;
    @Autowired ChunkSetRepository chunkSets;
    @Autowired DocumentRepository documents;

    private static float[] unit(int axis) {
        float[] v = new float[768];
        v[axis] = 1f;
        return v;
    }

    private static Chunk chunk(String text) {
        return new Chunk(0, text.length(), 1, 1, text);
    }

    @Test
    void returnsNearestChunkFirstAndKeepsChunkSetsIsolated() {
        Document doc = documents.save(new Document("t.pdf", 1, 100, UUID.randomUUID().toString().replace("-", "")));
        ChunkSet a = chunkSets.saveAndFlush(new ChunkSet(doc.getId(), "a", "recursive", 1000, 0, "test", 768, 3));
        ChunkSet b = chunkSets.saveAndFlush(new ChunkSet(doc.getId(), "b", "fixed", 1000, 0, "test", 768, 1));

        vectors.insertAll(a.getId(), List.of(chunk("north"), chunk("east"), chunk("south")),
                List.of(unit(0), unit(1), unit(2)));
        vectors.insertAll(b.getId(), List.of(chunk("other-set")), List.of(unit(1)));

        List<ScoredChunk> hits = vectors.search(a.getId(), unit(1), 3);

        assertThat(hits).hasSize(3);                                  // nothing from set b, and still 3 rows
        assertThat(hits.get(0).content()).isEqualTo("east");
        assertThat(hits.get(0).score()).isCloseTo(1.0, within(1e-4));
        assertThat(hits).extracting(ScoredChunk::content).doesNotContain("other-set");
    }
}