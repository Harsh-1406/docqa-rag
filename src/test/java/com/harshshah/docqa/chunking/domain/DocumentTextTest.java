package com.harshshah.docqa.chunking.domain;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DocumentTextTest {

    private final DocumentText doc = DocumentText.fromTexts(List.of("aaaa", "bbbb", "cccc"));
    // text = "aaaa\n\nbbbb\n\ncccc"  -> page starts at 0, 6, 12

    @Test
    void mapsOffsetsToPages() {
        assertThat(doc.text()).isEqualTo("aaaa\n\nbbbb\n\ncccc");
        assertThat(doc.pageAt(0)).isEqualTo(1);
        assertThat(doc.pageAt(6)).isEqualTo(2);
        assertThat(doc.pageAt(12)).isEqualTo(3);
    }

    @Test
    void sliceReportsPageRangeAndTrimsWhitespace() {
        Chunk spanning = doc.slice(2, 8).orElseThrow();      // "aa\n\nbb"
        assertThat(spanning.startPage()).isEqualTo(1);
        assertThat(spanning.endPage()).isEqualTo(2);

        Chunk trimmed = doc.slice(4, 12).orElseThrow();      // "\n\nbbbb\n\n"
        assertThat(trimmed.text()).isEqualTo("bbbb");
        assertThat(trimmed.startChar()).isEqualTo(6);
        assertThat(trimmed.endChar()).isEqualTo(10);
        assertThat(trimmed.startPage()).isEqualTo(2);
        assertThat(trimmed.endPage()).isEqualTo(2);
    }

    @Test
    void blankSliceIsEmpty() {
        assertThat(doc.slice(4, 6)).isEmpty();
    }

    @Test
    void globalOffsetConvertsPageOffsets() {
        assertThat(doc.globalOffset(2, 1)).isEqualTo(7);
    }

    @Test
    void splitsRangesAtPageBoundaries() {
        List<int[]> segs = doc.pageSegments(2, 14);

        assertThat(segs).extracting(s -> s[0]).containsExactly(1, 2, 3);
        assertThat(segs.get(0)).containsExactly(1, 2, 6);
        assertThat(segs.get(2)).containsExactly(3, 12, 14);
    }
}