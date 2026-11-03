package com.harshshah.docqa.embedding;

import com.harshshah.docqa.common.EmbeddingException;

import java.util.ArrayDeque;
import java.util.function.LongSupplier;

/** Sliding 60-second window limiting both requests and (estimated) tokens. Blocks until a call is allowed. */
public class TokenRateLimiter {

    interface Sleeper {
        void sleep(long millis);
    }

    private record Event(long timeMillis, int tokens, int requests) {}

    private static final long WINDOW_MS = 60_000;

    private final int maxRequests;
    private final int maxTokens;
    private final LongSupplier clock;
    private final Sleeper sleeper;
    private final ArrayDeque<Event> window = new ArrayDeque<>();

    public TokenRateLimiter(int maxRequests, int maxTokens) {
        this(maxRequests, maxTokens, System::currentTimeMillis, millis -> {
            try {
                Thread.sleep(millis);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new EmbeddingException("Interrupted while waiting for rate limit");
            }
        });
    }

    TokenRateLimiter(int maxRequests, int maxTokens, LongSupplier clock, Sleeper sleeper) {
        this.maxRequests = maxRequests;
        this.maxTokens = maxTokens;
        this.clock = clock;
        this.sleeper = sleeper;
    }

    public synchronized void acquire(int tokens) {
        acquire(tokens, 1);
    }

    /** requests = number of texts in the call, because the API counts each text as one request. */
    public synchronized void acquire(int tokens, int requests) {
        int needTokens = Math.min(tokens, maxTokens);
        int needRequests = Math.min(requests, maxRequests);
        while (true) {
            long now = clock.getAsLong();
            while (!window.isEmpty() && now - window.peekFirst().timeMillis() >= WINDOW_MS) {
                window.pollFirst();
            }
            int usedTokens = window.stream().mapToInt(Event::tokens).sum();
            int usedRequests = window.stream().mapToInt(Event::requests).sum();
            if (usedRequests + needRequests <= maxRequests && usedTokens + needTokens <= maxTokens) {
                window.addLast(new Event(now, needTokens, needRequests));
                return;
            }
            long wait = WINDOW_MS - (now - window.peekFirst().timeMillis());
            sleeper.sleep(Math.max(wait, 50));
        }
    }
}