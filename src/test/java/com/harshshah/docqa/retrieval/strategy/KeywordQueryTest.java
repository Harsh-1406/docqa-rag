package com.harshshah.docqa.retrieval.strategy;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class KeywordQueryTest {

    @Test
    void joinsDistinctTermsWithOr() {
        assertThat(KeywordQuery.toTsQuery("What was diluted EPS in 2025? Diluted!"))
                .isEqualTo("what | was | diluted | eps | in | 2025");
    }

    @Test
    void stripsCharactersThatBreakTsQuerySyntax() {
        assertThat(KeywordQuery.toTsQuery("Apple's (net) sales: & | ! <->")).isEqualTo("apple | s | net | sales");
    }

    @Test
    void returnsEmptyWhenThereAreNoTerms() {
        assertThat(KeywordQuery.toTsQuery("?!  ...")).isEmpty();
    }
}