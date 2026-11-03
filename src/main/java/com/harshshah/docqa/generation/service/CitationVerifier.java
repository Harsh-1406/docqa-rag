package com.harshshah.docqa.generation.service;

import java.util.Locale;

public final class CitationVerifier {

    private CitationVerifier() {}

    /** True if the quote appears on the page, ignoring case, whitespace runs, and smart punctuation. */
    public static boolean quoteOnPage(String quote, String pageText) {
        if (quote == null || pageText == null) return false;
        String q = normalize(quote);
        return !q.isEmpty() && normalize(pageText).contains(q);
    }

    static String normalize(String s) {
        return s.replace('\u2019', '\'').replace('\u2018', '\'')
                .replace('\u201C', '"').replace('\u201D', '"')
                .replace('\u2013', '-').replace('\u2014', '-')
                .replaceAll("\\s+", " ")
                .trim()
                .toLowerCase(Locale.ROOT);
    }
}