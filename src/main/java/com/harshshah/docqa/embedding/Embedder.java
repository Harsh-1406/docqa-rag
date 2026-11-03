package com.harshshah.docqa.embedding;

import com.harshshah.docqa.chunking.strategy.TextEmbedder;

import java.util.List;

/** Chunks and queries are embedded differently (task types), so the two operations are separate. */
public interface Embedder extends TextEmbedder {

    List<float[]> embedDocuments(List<String> texts);

    float[] embedQuery(String query);

    String modelName();

    int dimensions();

    /** Used by SemanticChunker. */
    @Override
    default List<float[]> embed(List<String> texts) {
        return embedDocuments(texts);
    }
}