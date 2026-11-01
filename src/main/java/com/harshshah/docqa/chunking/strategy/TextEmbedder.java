package com.harshshah.docqa.chunking.strategy;

import java.util.List;

/** Turns texts into vectors. Implemented by the embedding client in Module 3. */
@FunctionalInterface
public interface TextEmbedder {
    List<float[]> embed(List<String> texts);
}