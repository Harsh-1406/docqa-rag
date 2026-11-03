package com.harshshah.docqa.store.domain;

/** score is cosine similarity (1.0 = identical direction). */
public record ScoredChunk(int chunkIndex, int startPage, int endPage, int startChar, int endChar,
                          String content, double score) {}