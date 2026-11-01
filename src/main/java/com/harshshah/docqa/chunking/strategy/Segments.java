package com.harshshah.docqa.chunking.strategy;

import java.util.List;

final class Segments {

    private Segments() {}

    /** Moves a cut point back to just before a whitespace char, but never below start + minLen. */
    static int snapToWhitespace(String t, int start, int end, int minLen) {
        if (end >= t.length()) return end;
        for (int i = end; i > start + minLen; i--) {
            if (Character.isWhitespace(t.charAt(i))) return i;
        }
        return end;
    }

    /** Splits [start, end) into windows of at most maxSize, preferring word boundaries. */
    static void hardCut(String t, int start, int end, int maxSize, List<int[]> out) {
        int pos = start;
        while (pos < end) {
            int cut = Math.min(pos + maxSize, end);
            if (cut < end) cut = snapToWhitespace(t, pos, cut, maxSize / 2);
            out.add(new int[]{pos, cut});
            pos = cut;
        }
    }
}