package com.harshshah.docqa.retrieval.strategy;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Turns a natural-language question into a safe OR query for to_tsquery. Only letters and digits survive. */
final class KeywordQuery {

    private static final Pattern TOKEN = Pattern.compile("[\\p{L}\\p{N}]+");

    private KeywordQuery() {}

    /** Returns "" if the question has no usable terms. */
    static String toTsQuery(String question) {
        Set<String> terms = new LinkedHashSet<>();
        Matcher m = TOKEN.matcher(question);
        while (m.find()) {
            terms.add(m.group().toLowerCase(Locale.ROOT));
        }
        return String.join(" | ", terms);
    }
}