package com.harshshah.docqa.retrieval.api;

import java.util.List;

public record SearchResponse(String label, String mode, long millis, List<Hit> hits) {

    public record Hit(int rank, double score, int startPage, int endPage, int startChar, int endChar, String text) {}
}