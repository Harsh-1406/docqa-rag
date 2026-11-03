package com.harshshah.docqa.store.domain;

import com.harshshah.docqa.chunking.domain.Chunk;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Hibernate cannot map pgvector's `vector` type, so this repository uses plain SQL. */
@Repository
@RequiredArgsConstructor
public class ChunkVectorRepository {

    private final JdbcTemplate jdbc;

    public void insertAll(UUID chunkSetId, List<Chunk> chunks, List<float[]> vectors) {
        if (chunks.size() != vectors.size()) {
            throw new IllegalArgumentException("chunks and vectors must have the same size");
        }
        List<Object[]> rows = new ArrayList<>(chunks.size());
        for (int i = 0; i < chunks.size(); i++) {
            Chunk c = chunks.get(i);
            rows.add(new Object[]{chunkSetId, i, c.startChar(), c.endChar(), c.startPage(), c.endPage(),
                    c.text(), toLiteral(vectors.get(i))});
        }
        jdbc.batchUpdate("""
                INSERT INTO chunks (chunk_set_id, chunk_index, start_char, end_char, start_page, end_page, content, embedding)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?::vector)
                """, rows);
    }

    /**
     * Nearest chunks by cosine distance within one chunk set.
     * The HNSW index spans all chunk sets, so a plain WHERE would filter AFTER the index returns its candidates and
     * could return fewer than k rows. Iterative scan (pgvector 0.8+) keeps scanning until enough rows match.
     */
    @Transactional(readOnly = true)
    public List<ScoredChunk> search(UUID chunkSetId, float[] query, int k) {
        jdbc.execute("SET LOCAL hnsw.iterative_scan = relaxed_order");
        jdbc.execute("SET LOCAL hnsw.ef_search = 100");
        String vec = toLiteral(query);
        return jdbc.query("""
                SELECT chunk_index, start_page, end_page, start_char, end_char, content, 1 - distance AS score
                FROM (
                    SELECT chunk_index, start_page, end_page, start_char, end_char, content,
                           embedding <=> ?::vector AS distance
                    FROM chunks
                    WHERE chunk_set_id = ?
                    ORDER BY embedding <=> ?::vector
                    LIMIT ?
                ) nearest
                ORDER BY distance
                """,
                (rs, i) -> new ScoredChunk(rs.getInt("chunk_index"), rs.getInt("start_page"), rs.getInt("end_page"),
                        rs.getInt("start_char"), rs.getInt("end_char"), rs.getString("content"),
                        rs.getDouble("score")),
                vec, chunkSetId, vec, k);
    }

    static String toLiteral(float[] v) {
        StringBuilder sb = new StringBuilder(v.length * 10).append('[');
        for (int i = 0; i < v.length; i++) {
            if (i > 0) sb.append(',');
            sb.append(v[i]);
        }
        return sb.append(']').toString();
    }
}