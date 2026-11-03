package com.harshshah.docqa.embedding;

import com.harshshah.docqa.common.EmbeddingException;
import com.harshshah.docqa.config.GeminiProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class GeminiEmbedder implements Embedder {

    record Part(String text) {}
    record Content(List<Part> parts) {}
    record EmbedRequest(String model, Content content, String taskType, int outputDimensionality) {}
    record BatchRequest(List<EmbedRequest> requests) {}
    record Embedding(float[] values) {}
    record BatchResponse(List<Embedding> embeddings) {}

    private final GeminiProperties props;
    private final RestClient http;
    private final TokenRateLimiter limiter;

    public GeminiEmbedder(GeminiProperties props) {
        this.props = props;
        var factory = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build());
        factory.setReadTimeout(Duration.ofSeconds(60));
        this.http = RestClient.builder().baseUrl(props.baseUrl()).requestFactory(factory).build();
        this.limiter = new TokenRateLimiter(props.maxRequestsPerMinute(), props.maxTokensPerMinute());
    }

    @Override
    public List<float[]> embedDocuments(List<String> texts) {
        return embedAll(texts, "RETRIEVAL_DOCUMENT");
    }

    @Override
    public float[] embedQuery(String query) {
        return embedAll(List.of(query), "RETRIEVAL_QUERY").get(0);
    }

    @Override
    public String modelName() {
        return props.model();
    }

    @Override
    public int dimensions() {
        return props.dimensions();
    }

    private List<float[]> embedAll(List<String> texts, String taskType) {
        if (props.apiKey() == null || props.apiKey().isBlank()) {
            throw new EmbeddingException("GEMINI_API_KEY is not set. Export it before starting the app.");
        }
        int tokenBudget = props.maxTokensPerMinute() / 2;   // one request never uses more than half a minute's budget
        List<float[]> out = new ArrayList<>(texts.size());
        List<String> batch = new ArrayList<>();
        int batchTokens = 0;
        for (String t : texts) {
            int est = estimateTokens(t);
            if (!batch.isEmpty() && (batch.size() >= props.maxBatchSize() || batchTokens + est > tokenBudget)) {
                out.addAll(send(batch, taskType, batchTokens));
                log.info("Embedded {}/{} texts", out.size(), texts.size());
                batch.clear();
                batchTokens = 0;
            }
            batch.add(t);
            batchTokens += est;
        }
        if (!batch.isEmpty()) {
            out.addAll(send(batch, taskType, batchTokens));
            log.info("Embedded {}/{} texts", out.size(), texts.size());
        }
        return out;
    }

    private List<float[]> send(List<String> batch, String taskType, int estimatedTokens) {
        var body = new BatchRequest(batch.stream()
                .map(t -> new EmbedRequest("models/" + props.model(), new Content(List.of(new Part(t))),
                        taskType, props.dimensions()))
                .toList());

        for (int attempt = 1; ; attempt++) {
            limiter.acquire(estimatedTokens);
            try {
                BatchResponse response = http.post()
                        .uri("/models/{model}:batchEmbedContents", props.model())
                        .header("x-goog-api-key", props.apiKey())
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(body)
                        .retrieve()
                        .body(BatchResponse.class);
                if (response == null || response.embeddings() == null
                        || response.embeddings().size() != batch.size()) {
                    throw new EmbeddingException("Gemini returned an unexpected number of embeddings.");
                }
                return response.embeddings().stream().map(e -> VectorMath.normalize(e.values())).toList();
            } catch (RestClientResponseException e) {
                int status = e.getStatusCode().value();
                boolean retryable = status == 429 || status >= 500;
                if (!retryable || attempt >= props.maxRetries()) {
                    throw new EmbeddingException("Gemini embedding request failed (HTTP " + status + "): "
                            + truncate(e.getResponseBodyAsString()), e);
                }
                log.warn("Gemini returned HTTP {}; retry {}/{}", status, attempt, props.maxRetries());
            } catch (ResourceAccessException e) {
                if (attempt >= props.maxRetries()) {
                    throw new EmbeddingException("Could not reach Gemini: " + e.getMessage(), e);
                }
                log.warn("Network error talking to Gemini; retry {}/{}", attempt, props.maxRetries());
            }
            backoff(attempt);
        }
    }

    private static int estimateTokens(String text) {
        return (text.length() + 2) / 3;   // deliberately pessimistic: tables tokenise worse than prose
    }

    private static void backoff(int attempt) {
        try {
            Thread.sleep(Math.min(60_000L, 2_000L << attempt));
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new EmbeddingException("Interrupted while retrying");
        }
    }

    private static String truncate(String s) {
        return s == null ? "" : s.length() > 300 ? s.substring(0, 300) + "..." : s;
    }
}