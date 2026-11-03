package com.harshshah.docqa.embedding;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

class TokenRateLimiterTest {

    private final AtomicLong now = new AtomicLong(0);
    private final AtomicLong totalSlept = new AtomicLong(0);

    private TokenRateLimiter limiter(int maxRequests, int maxTokens) {
        return new TokenRateLimiter(maxRequests, maxTokens, now::get, ms -> {
            totalSlept.addAndGet(ms);
            now.addAndGet(ms);
        });
    }

    @Test
    void allowsCallsWithinBothLimitsWithoutWaiting() {
        var limiter = limiter(3, 1000);
        limiter.acquire(300);
        limiter.acquire(300);
        limiter.acquire(300);
        assertThat(totalSlept.get()).isZero();
    }

    @Test
    void waitsForTheWindowWhenRequestLimitIsHit() {
        var limiter = limiter(2, 100_000);
        limiter.acquire(10);
        limiter.acquire(10);
        limiter.acquire(10);   // third call must wait until the first leaves the window
        assertThat(now.get()).isGreaterThanOrEqualTo(60_000);
    }

    @Test
    void waitsForTheWindowWhenTokenLimitIsHit() {
        var limiter = limiter(100, 1000);
        limiter.acquire(600);
        limiter.acquire(600);  // 1200 > 1000, so it waits
        assertThat(now.get()).isGreaterThanOrEqualTo(60_000);
    }

    @Test
    void countsEachTextInABatchAsOneRequest() {
        var limiter = limiter(80, 1_000_000);
        limiter.acquire(10, 50);
        limiter.acquire(10, 50);   // 100 texts > 80 per minute, so it waits
        assertThat(now.get()).isGreaterThanOrEqualTo(60_000);
    }
}