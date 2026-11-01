package com.harshshah.docqa.chunking.strategy;

import com.harshshah.docqa.chunking.domain.Chunk;
import com.harshshah.docqa.chunking.domain.DocumentText;

import java.util.List;

public interface Chunker {
    /** Stable label used for DB table names and eval reports, e.g. "recursive-2000-o200". */
    String name();

    List<Chunk> chunk(DocumentText document);
}