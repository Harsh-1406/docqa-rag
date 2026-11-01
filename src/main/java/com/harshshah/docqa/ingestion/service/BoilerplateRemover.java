package com.harshshah.docqa.ingestion.service;

import lombok.extern.slf4j.Slf4j;

import java.util.*;

/**
 * Removes lines that repeat at the top or bottom of many pages (running headers,
 * footers, page numbers). Digits are normalised, so "Report | 37" and "Report | 38"
 * count as the same pattern.
 */
@Slf4j
public final class BoilerplateRemover {

    private static final int EDGE_LINES = 2;          // first/last N non-blank lines per page
    private static final int MAX_LINE_LENGTH = 120;   // headers/footers are short
    private static final double MIN_PAGE_FRACTION = 0.3;
    private static final int MIN_PAGES = 3;

    private BoilerplateRemover() {}

    public static List<String> strip(List<String> pages) {
        if (pages.size() < MIN_PAGES) {
            return pages;
        }
        int threshold = Math.max(MIN_PAGES, (int) Math.ceil(pages.size() * MIN_PAGE_FRACTION));
        List<String[]> split = pages.stream().map(p -> p.split("\n", -1)).toList();

        Map<String, Integer> pageCounts = new HashMap<>();
        for (String[] lines : split) {
            Set<String> keys = new HashSet<>();
            for (int idx : edgeIndexes(lines)) {
                String key = key(lines[idx]);
                if (key != null) keys.add(key);
            }
            keys.forEach(k -> pageCounts.merge(k, 1, Integer::sum));
        }

        Set<String> boilerplate = new HashSet<>();
        pageCounts.forEach((k, c) -> { if (c >= threshold) boilerplate.add(k); });
        if (boilerplate.isEmpty()) {
            return pages;
        }
        log.info("Removing repeated header/footer patterns: {}", boilerplate);

        List<String> result = new ArrayList<>(pages.size());
        for (String[] lines : split) {
            Set<Integer> drop = new HashSet<>();
            for (int idx : edgeIndexes(lines)) {
                String key = key(lines[idx]);
                if (key != null && boilerplate.contains(key)) drop.add(idx);
            }
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < lines.length; i++) {
                if (!drop.contains(i)) sb.append(lines[i]).append('\n');
            }
            result.add(PdfTextExtractor.clean(sb.toString()));
        }
        return result;
    }

    private static List<Integer> edgeIndexes(String[] lines) {
        List<Integer> nonBlank = new ArrayList<>();
        for (int i = 0; i < lines.length; i++) {
            if (!lines[i].isBlank()) nonBlank.add(i);
        }
        Set<Integer> edge = new LinkedHashSet<>();
        for (int k = 0; k < Math.min(EDGE_LINES, nonBlank.size()); k++) {
            edge.add(nonBlank.get(k));
            edge.add(nonBlank.get(nonBlank.size() - 1 - k));
        }
        return new ArrayList<>(edge);
    }

    private static String key(String line) {
        String t = line.trim();
        if (t.isEmpty() || t.length() > MAX_LINE_LENGTH) return null;
        return t.replaceAll("\\d+", "#").replaceAll("\\s+", " ");
    }
}