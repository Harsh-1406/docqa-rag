package com.harshshah.docqa.chunking.domain;

import com.harshshah.docqa.ingestion.domain.DocumentPage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/** All pages of a document joined into one string, with a page lookup for any character offset. */
public final class DocumentText {

    private static final String PAGE_SEPARATOR = "\n\n";

    private final String text;
    private final int[] pageNumbers;   // ascending
    private final int[] pageStarts;    // global offset where each page begins

    private DocumentText(String text, int[] pageNumbers, int[] pageStarts) {
        this.text = text;
        this.pageNumbers = pageNumbers;
        this.pageStarts = pageStarts;
    }

    public static DocumentText fromPages(List<DocumentPage> pages) {
        int[] numbers = pages.stream().mapToInt(DocumentPage::getPageNumber).toArray();
        return build(numbers, pages.stream().map(DocumentPage::getText).toList());
    }

    /** Convenience for tests: pages are numbered 1..n. */
    public static DocumentText fromTexts(List<String> texts) {
        int[] numbers = new int[texts.size()];
        for (int i = 0; i < numbers.length; i++) numbers[i] = i + 1;
        return build(numbers, texts);
    }

    private static DocumentText build(int[] numbers, List<String> texts) {
        StringBuilder sb = new StringBuilder();
        int[] starts = new int[texts.size()];
        for (int i = 0; i < texts.size(); i++) {
            if (i > 0) sb.append(PAGE_SEPARATOR);
            starts[i] = sb.length();
            sb.append(texts.get(i));
        }
        return new DocumentText(sb.toString(), numbers, starts);
    }

    public String text() {
        return text;
    }

    /** PDF page number that contains the character at this global offset. */
    public int pageAt(int offset) {
        int idx = Arrays.binarySearch(pageStarts, offset);
        if (idx < 0) idx = Math.max(0, -idx - 2);
        return pageNumbers[idx];
    }

    /** Global offset of a character given its page number and offset within that page (used by the eval harness). */
    public int globalOffset(int pageNumber, int charInPage) {
        int idx = Arrays.binarySearch(pageNumbers, pageNumber);
        if (idx < 0) throw new IllegalArgumentException("No such page: " + pageNumber);
        return pageStarts[idx] + charInPage;
    }

    /** Builds a chunk for [start, end), trimming whitespace. Empty if the slice is blank. */
    public Optional<Chunk> slice(int start, int end) {
        while (start < end && Character.isWhitespace(text.charAt(start))) start++;
        while (end > start && Character.isWhitespace(text.charAt(end - 1))) end--;
        if (start >= end) return Optional.empty();
        return Optional.of(new Chunk(start, end, pageAt(start), pageAt(end - 1), text.substring(start, end)));
    }

    /** Splits [start, end) at page boundaries. Each entry is {pageNumber, from, to}. */
    public List<int[]> pageSegments(int start, int end) {
        List<int[]> out = new ArrayList<>();
        int idx = Arrays.binarySearch(pageStarts, start);
        if (idx < 0) idx = Math.max(0, -idx - 2);
        for (int i = idx; i < pageStarts.length && pageStarts[i] < end; i++) {
            int from = Math.max(start, pageStarts[i]);
            int to = Math.min(end, i + 1 < pageStarts.length ? pageStarts[i + 1] : text.length());
            if (from < to) out.add(new int[]{pageNumbers[i], from, to});
        }
        return out;
    }
}