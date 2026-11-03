package com.harshshah.docqa.store.domain;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class ChunkTextRepository {

    private static final RowMapper<ScoredChunk> MAPPER = (rs, i) -> new ScoredChunk(
            rs.getInt("chunk_index"), rs.getInt("start_page"), rs.getInt("end_page"),
            rs.getInt("start_char"), rs.getInt("end_char"), rs.getString("content"), rs.getDouble("score"));

    private final JdbcTemplate jdbc;

    /** tsQuery is a to_tsquery expression such as "diluted | earnings | share". */
    public List<ScoredChunk> search(UUID chunkSetId, String tsQuery, int k) {
        return jdbc.query("""
                SELECT c.chunk_index, c.start_page, c.end_page, c.start_char, c.end_char, c.content,
                       ts_rank_cd(c.content_tsv, q.query) AS score
                FROM chunks c, to_tsquery('english', ?::text) AS q(query)
                WHERE c.chunk_set_id = ? AND c.content_tsv @@ q.query
                ORDER BY score DESC, c.chunk_index
                LIMIT ?
                """, MAPPER, tsQuery, chunkSetId, k);
    }
}