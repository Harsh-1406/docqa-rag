package com.harshshah.docqa.generation.llm;

public record LlmResponse(String text, String model, int inputTokens, int outputTokens, long millis) {}