package com.harshshah.docqa.generation.llm;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.harshshah.docqa.common.LlmException;
import com.harshshah.docqa.config.GeminiProperties;
import com.harshshah.docqa.config.GenerationProperties;
import com.harshshah.docqa.embedding.TokenRateLimiter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class GeminiChatClient implements LlmClient {

    record Part(String text) {}
    record Content(List<Part> parts) {}
    record GenerationConfig(String responseMimeType, Map<String, Object> responseSchema,
                            double temperature, int maxOutputTokens) {}
    record GenerateRequest(Content systemInstruction, List<Content> contents, GenerationConfig generationConfig) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ResponsePart(String text) {}
    @JsonIgnoreProperties(ignoreUnknown = true)
    record ResponseContent(List<ResponsePart> parts) {}
    @JsonIgnoreProperties(ignoreUnknown = true)
    record Candidate(ResponseContent content, String finishReason) {}
    @JsonIgnoreProperties(ignoreUnknown = true)
    record Usage(int promptTokenCount, int candidatesTokenCount, int thoughtsTokenCount) {}
    @JsonIgnoreProperties(ignoreUnknown = true)
    record GenerateResponse(List<Candidate> candidates, Usage usageMetadata, String modelVersion) {}

    private final GeminiProperties gemini;
    private final GenerationProperties gen;
    private final RestClient http;
    private final TokenRateLimiter limiter;

    public GeminiChatClient(GeminiProperties gemini, GenerationProperties gen) {
        this.gemini = gemini;
        this.gen = gen;
        var factory = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build());
        factory.setReadTimeout(Duration.ofSeconds(90));
        this.http = RestClient.builder().baseUrl(gemini.baseUrl()).requestFactory(factory).build();
        this.limiter = new TokenRateLimiter(gen.maxRequestsPerMinute(), gen.maxTokensPerMinute());
    }

    @Override
    public LlmResponse generateJson(String system, String user, Map<String, Object> schema) {
        if (gemini.apiKey() == null || gemini.apiKey().isBlank()) {
            throw new LlmException("GEMINI_API_KEY is not set. Export it before starting the app.");
        }
        var body = new GenerateRequest(
                new Content(List.of(new Part(system))),
                List.of(new Content(List.of(new Part(user)))),
                new GenerationConfig("application/json", schema, gen.temperature(), gen.maxOutputTokens()));
        int estimate = (system.length() + user.length() + 2) / 3 + gen.maxOutputTokens();

        for (int attempt = 1; ; attempt++) {
            limiter.acquire(estimate, 1);
            long start = System.nanoTime();
            try {
                GenerateResponse r = http.post()
                        .uri("/models/{model}:generateContent", gen.model())
                        .header("x-goog-api-key", gemini.apiKey())
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(body)
                        .retrieve()
                        .body(GenerateResponse.class);
                return toResponse(r, (System.nanoTime() - start) / 1_000_000);
            } catch (RestClientResponseException e) {
                int status = e.getStatusCode().value();
                boolean retryable = status == 429 || status >= 500;
                if (!retryable || attempt >= gen.maxRetries()) {
                    throw new LlmException("Gemini generation failed (HTTP " + status + "): "
                            + truncate(e.getResponseBodyAsString()), e);
                }
                log.warn("Gemini returned HTTP {}; retry {}/{}", status, attempt, gen.maxRetries());
            } catch (ResourceAccessException e) {
                if (attempt >= gen.maxRetries()) {
                    throw new LlmException("Could not reach Gemini: " + e.getMessage(), e);
                }
                log.warn("Network error talking to Gemini; retry {}/{}", attempt, gen.maxRetries());
            }
            backoff(attempt);
        }
    }

    private LlmResponse toResponse(GenerateResponse r, long millis) {
        if (r == null || r.candidates() == null || r.candidates().isEmpty()
                || r.candidates().get(0).content() == null || r.candidates().get(0).content().parts() == null) {
            throw new LlmException("Gemini returned no answer (the response may have been blocked).");
        }
        Candidate c = r.candidates().get(0);
        String text = c.content().parts().stream()
                .map(ResponsePart::text).filter(t -> t != null).reduce("", String::concat);
        if ("MAX_TOKENS".equals(c.finishReason())) {
            throw new LlmException("Gemini output was cut off (max-output-tokens too small).");
        }
        Usage u = r.usageMetadata();
        int in = u == null ? 0 : u.promptTokenCount();
        int out = u == null ? 0 : u.candidatesTokenCount() + u.thoughtsTokenCount();
        return new LlmResponse(text, r.modelVersion() != null ? r.modelVersion() : gen.model(), in, out, millis);
    }

    private static void backoff(int attempt) {
        try {
            Thread.sleep(Math.min(60_000L, 2_000L << attempt));
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new LlmException("Interrupted while retrying");
        }
    }

    private static String truncate(String s) {
        return s == null ? "" : s.length() > 300 ? s.substring(0, 300) + "..." : s;
    }
}