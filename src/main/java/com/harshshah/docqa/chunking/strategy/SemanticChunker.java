package com.harshshah.docqa.chunking.strategy;

import com.harshshah.docqa.chunking.domain.Chunk;
import com.harshshah.docqa.chunking.domain.DocumentText;

import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Embeds every sentence (with `bufferSize` neighbours on each side for context), measures the cosine
 * distance between consecutive sentences, and starts a new chunk where the distance is in the top
 * (100 - breakpointPercentile)% of all distances. minSize and maxSize bound the result.
 */
public class SemanticChunker implements Chunker {

    private static final int MAX_EMBED_CHARS = 6000;

    private final TextEmbedder embedder;
    private final int maxSize;
    private final int minSize;
    private final int bufferSize;
    private final double breakpointPercentile;

    public SemanticChunker(TextEmbedder embedder, int maxSize, int minSize,
                           int bufferSize, double breakpointPercentile) {
        this.embedder = embedder;
        this.maxSize = maxSize;
        this.minSize = minSize;
        this.bufferSize = bufferSize;
        this.breakpointPercentile = breakpointPercentile;
    }

    @Override
    public String name() {
        return "semantic-" + maxSize;
    }

    @Override
    public List<Chunk> chunk(DocumentText doc) {
        String t = doc.text();
        List<int[]> units = sentenceUnits(t);
        if (units.isEmpty()) return List.of();

        List<float[]> vectors = embedder.embed(embeddingWindows(t, units));
        double[] distance = new double[units.size() - 1];
        for (int i = 0; i < distance.length; i++) {
            distance[i] = 1.0 - cosine(vectors.get(i), vectors.get(i + 1));
        }
        double threshold = percentile(distance, breakpointPercentile);

        List<Chunk> out = new ArrayList<>();
        int groupStart = units.get(0)[0];
        int groupEnd = units.get(0)[1];
        for (int i = 1; i < units.size(); i++) {
            int[] u = units.get(i);
            boolean semanticBreak = distance[i - 1] > threshold && groupEnd - groupStart >= minSize;
            boolean tooBig = u[1] - groupStart > maxSize;
            if (semanticBreak || tooBig) {
                doc.slice(groupStart, groupEnd).ifPresent(out::add);
                groupStart = u[0];
            }
            groupEnd = u[1];
        }
        doc.slice(groupStart, groupEnd).ifPresent(out::add);
        return out;
    }

    /** Sentence spans; any sentence longer than maxSize (e.g. a table) is hard-cut. */
    private List<int[]> sentenceUnits(String t) {
        BreakIterator it = BreakIterator.getSentenceInstance(Locale.ENGLISH);
        it.setText(t);
        List<int[]> units = new ArrayList<>();
        int start = it.first();
        for (int end = it.next(); end != BreakIterator.DONE; start = end, end = it.next()) {
            if (t.substring(start, end).isBlank()) continue;
            Segments.hardCut(t, start, end, maxSize, units);
        }
        return units;
    }

    private List<String> embeddingWindows(String t, List<int[]> units) {
        List<String> windows = new ArrayList<>(units.size());
        for (int i = 0; i < units.size(); i++) {
            int from = units.get(Math.max(0, i - bufferSize))[0];
            int to = units.get(Math.min(units.size() - 1, i + bufferSize))[1];
            if (to - from > MAX_EMBED_CHARS) {
                from = units.get(i)[0];
                to = units.get(i)[1];
            }
            windows.add(t.substring(from, to));
        }
        return windows;
    }

    private static double cosine(float[] a, float[] b) {
        double dot = 0, na = 0, nb = 0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            na += a[i] * a[i];
            nb += b[i] * b[i];
        }
        double denom = Math.sqrt(na) * Math.sqrt(nb);
        return denom == 0 ? 0 : dot / denom;
    }

    private static double percentile(double[] values, double p) {
        if (values.length == 0) return Double.POSITIVE_INFINITY;
        double[] sorted = values.clone();
        Arrays.sort(sorted);
        int idx = (int) Math.ceil(p / 100.0 * sorted.length) - 1;
        return sorted[Math.max(0, Math.min(idx, sorted.length - 1))];
    }
}