package com.harshshah.docqa.chunking.domain;

/** A slice of the document. Offsets are into DocumentText.text(); text == text().substring(startChar, endChar). */
public record Chunk(int startChar, int endChar, int startPage, int endPage, String text) {
    public int length() {
        return text.length();
    }
}