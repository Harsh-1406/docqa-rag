package com.harshshah.docqa.chunking.strategy;

import com.harshshah.docqa.chunking.domain.Chunk;
import com.harshshah.docqa.chunking.domain.DocumentText;

import java.util.ArrayList;
import java.util.List;

/**
 * Structure-aware: splits on the coarsest separator that works (paragraph, line, sentence, word),
 * then greedily merges neighbouring pieces up to maxSize. Overlap repeats trailing pieces.
 */
public class RecursiveChunker implements Chunker {

    private static final String[] SEPARATORS = {"\n\n", "\n", ". ", " "};

    private final int maxSize;
    private final int overlap;

    public RecursiveChunker(int maxSize, int overlap) {
        this.maxSize = maxSize;
        this.overlap = overlap;
    }

    @Override
    public String name() {
        return "recursive-" + maxSize + "-o" + overlap;
    }

    @Override
    public List<Chunk> chunk(DocumentText doc) {
        String t = doc.text();
        List<int[]> segs = new ArrayList<>();
        split(t, 0, t.length(), 0, segs);

        List<Chunk> out = new ArrayList<>();
        int i = 0;
        while (i < segs.size()) {
            int start = segs.get(i)[0];
            int j = i;
            while (j + 1 < segs.size() && segs.get(j + 1)[1] - start <= maxSize) j++;
            int end = segs.get(j)[1];
            doc.slice(start, end).ifPresent(out::add);
            if (j + 1 >= segs.size()) break;

            // step back over trailing segments that fit in the overlap, but always move forward
            int next = j + 1;
            while (next - 1 > i && end - segs.get(next - 1)[0] <= overlap) next--;
            i = next;
        }
        return out;
    }

    /** Produces contiguous segments of at most maxSize covering [start, end). */
    private void split(String t, int start, int end, int level, List<int[]> out) {
        if (end - start <= maxSize) {
            out.add(new int[]{start, end});
            return;
        }
        if (level >= SEPARATORS.length) {
            Segments.hardCut(t, start, end, maxSize, out);
            return;
        }
        String sep = SEPARATORS[level];
        int pos = start;
        while (pos < end) {
            int idx = t.indexOf(sep, pos);
            int pieceEnd = (idx < 0 || idx >= end) ? end : Math.min(idx + sep.length(), end);
            split(t, pos, pieceEnd, level + 1, out);
            pos = pieceEnd;
        }
    }
}