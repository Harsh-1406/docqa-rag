package com.harshshah.docqa.ingestion.service;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

class BoilerplateRemoverTest {

    private static String word(int i) {
        return String.valueOf((char) ('A' + i)).repeat(6);   // AAAAAA, BBBBBB, ... (no digits)
    }

    @Test
    void removesRepeatedFooterButKeepsContent() {
        List<String> pages = IntStream.range(0, 10)
                .mapToObj(i -> word(i) + "\nbody text of " + word(i) + "\nAcme Corp | 2025 Report | " + (i + 1))
                .toList();

        List<String> cleaned = BoilerplateRemover.strip(pages);

        for (int i = 0; i < 10; i++) {
            assertThat(cleaned.get(i)).doesNotContain("Acme Corp");
            assertThat(cleaned.get(i)).contains(word(i));
        }
    }

    @Test
    void leavesPatternsThatRepeatOnlyOnAFewPages() {
        List<String> pages = IntStream.range(0, 10)
                .mapToObj(i -> word(i) + "\nbody text of " + word(i) + (i < 2 ? "\nRare Footer" : "\nlast " + word(i)))
                .toList();

        assertThat(BoilerplateRemover.strip(pages).get(0)).contains("Rare Footer");
    }

    @Test
    void shortDocumentsAreUntouched() {
        List<String> pages = List.of("Footer 1", "Footer 2");
        assertThat(BoilerplateRemover.strip(pages)).isEqualTo(pages);
    }
}