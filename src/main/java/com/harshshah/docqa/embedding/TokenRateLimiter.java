package com.harshshah.docqa.embedding;

import com.harshshah.docqa.common.EmbeddingException;

import java.util.ArrayDeque;
import java.util.function.LongSupplier;

/** Sliding 60-second window limiting both requests and (estimated) tokens. Blocks until a call is allowed. */
public class TokenRateLimiter {

    interface Sleeper {
        void sleep(long millis);
    }

    private record Event(long timeMillis, int tokens) {}

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
        int need = Math.min(tokens, maxTokens);
        while (true) {
            long now = clock.getAsLong();
            while (!window.isEmpty() && now - window.peekFirst().timeMillis() >= WINDOW_MS) {
                window.pollFirst();
            }
            int used = window.stream().mapToInt(Event::tokens).sum();
            if (window.size() < maxRequests && used + need <= maxTokens) {
                window.addLast(new Event(now, need));
                return;
            }
            long wait = WINDOW_MS - (now - window.peekFirst().timeMillis());
            sleeper.sleep(Math.max(wait, 50));
        }
    }
}