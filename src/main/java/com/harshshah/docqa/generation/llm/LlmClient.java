package com.harshshah.docqa.generation.llm;

import java.util.Map;

public interface LlmClient {
    LlmResponse generateJson(String systemPrompt, String userPrompt, Map<String, Object> responseSchema);
}