package com.harshshah.docqa.generation.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CitationVerifierTest {

    private static final String PAGE = "Total net sales  416,161\n 391,035\nThe Company\u2019s results";

    @Test
    void matchesIgnoringCaseWhitespaceAndSmartQuotes() {
        assertThat(CitationVerifier.quoteOnPage("total net sales 416,161 391,035", PAGE)).isTrue();
        assertThat(CitationVerifier.quoteOnPage("The Company's results", PAGE)).isTrue();
    }

    @Test
    void rejectsQuotesThatAreNotOnThePage() {
        assertThat(CitationVerifier.quoteOnPage("net sales were 500,000", PAGE)).isFalse();
    }

    @Test
    void rejectsBlankOrMissingInput() {
        assertThat(CitationVerifier.quoteOnPage("   ", PAGE)).isFalse();
        assertThat(CitationVerifier.quoteOnPage(null, PAGE)).isFalse();
        assertThat(CitationVerifier.quoteOnPage("x", null)).isFalse();
    }
}