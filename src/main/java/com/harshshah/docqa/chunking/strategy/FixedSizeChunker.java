package com.harshshah.docqa.chunking.strategy;

import com.harshshah.docqa.chunking.domain.Chunk;
import com.harshshah.docqa.chunking.domain.DocumentText;

import java.util.ArrayList;
import java.util.List;

/** Sliding window of `size` characters. overlap = 0 gives plain fixed-size chunking. Cuts land on word boundaries. */
public class FixedSizeChunker implements Chunker {

    private final int size;
    private final int overlap;

    public FixedSizeChunker(int size, int overlap) {
        this.size = size;
        this.overlap = overlap;
    }

    @Override
    public String name() {
        return "fixed-" + size + "-o" + overlap;
    }

    @Override
    public List<Chunk> chunk(DocumentText doc) {
        String t = doc.text();
        int n = t.length();
        List<Chunk> out = new ArrayList<>();
        int start = 0;
        while (start < n) {
            int end = Segments.snapToWhitespace(t, start, Math.min(start + size, n), size / 2);
            doc.slice(start, end).ifPresent(out::add);
            if (end >= n) break;

            start = (end - overlap > start) ? end - overlap : end;
            // don't start in the middle of a word
            while (start < end && start > 0
                    && !Character.isWhitespace(t.charAt(start - 1))
                    && !Character.isWhitespace(t.charAt(start))) {
                start++;
            }
        }
        return out;
    }
}