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

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class ChunkTextRepositoryTest {

    @Autowired ChunkTextRepository texts;
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
    void matchesStemmedTermsAndStaysInsideOneChunkSet() {
        Document doc = documents.save(new Document("k.pdf", 1, 100, UUID.randomUUID().toString().replace("-", "")));
        ChunkSet a = chunkSets.saveAndFlush(new ChunkSet(doc.getId(), "kw-a", "recursive", 1000, 0, "test", 768, 3));
        ChunkSet b = chunkSets.saveAndFlush(new ChunkSet(doc.getId(), "kw-b", "fixed", 1000, 0, "test", 768, 1));
        vectors.insertAll(a.getId(), List.of(
                        chunk("Diluted earnings per share were 7.46"),
                        chunk("Supply chain risks are described here"),
                        chunk("Total net sales increased")),
                List.of(unit(0), unit(1), unit(2)));
        vectors.insertAll(b.getId(), List.of(chunk("diluted diluted other set")), List.of(unit(0)));

        List<ScoredChunk> hits = texts.search(a.getId(), "dilution | earnings", 5);

        assertThat(hits).hasSize(1);
        assertThat(hits.get(0).content()).startsWith("Diluted earnings");
    }
}