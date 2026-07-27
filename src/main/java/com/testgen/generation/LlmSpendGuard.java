package com.testgen.generation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Wraps an {@link LlmProvider} via composition — LlmProvider is a sealed interface
 * permitting only Anthropic/OpenAi/Noop, so this cannot be a fourth permitted implementation.
 */
public class LlmSpendGuard {

    private static final Logger log = LoggerFactory.getLogger(LlmSpendGuard.class);
    private static final int CHARS_PER_TOKEN = 4;

    private final LlmProvider delegate;
    private final int maxTokensPerHour;
    private final Clock clock;
    private final ConcurrentHashMap<Instant, AtomicInteger> tokensByHour = new ConcurrentHashMap<>();

    public LlmSpendGuard(LlmProvider delegate, int maxTokensPerHour) {
        this(delegate, maxTokensPerHour, Clock.systemUTC());
    }

    LlmSpendGuard(LlmProvider delegate, int maxTokensPerHour, Clock clock) {
        this.delegate = delegate;
        this.maxTokensPerHour = maxTokensPerHour;
        this.clock = clock;
    }

    public String generate(String systemPrompt, String userPrompt) {
        int estimatedTokens = (systemPrompt.length() + userPrompt.length()) / CHARS_PER_TOKEN;
        Instant hour = Instant.now(clock).truncatedTo(ChronoUnit.HOURS);
        AtomicInteger hourlySpend = tokensByHour.computeIfAbsent(hour, h -> new AtomicInteger());

        int projectedSpend = hourlySpend.get() + estimatedTokens;
        if (projectedSpend > maxTokensPerHour) {
            log.warn("generate: rejected, {} tokens would bring hourly spend to {}/{}",
                    estimatedTokens, projectedSpend, maxTokensPerHour);
            throw new SpendLimitExceededException(projectedSpend, maxTokensPerHour);
        }

        String response = delegate.generate(systemPrompt, userPrompt);

        int cumulativeSpend = hourlySpend.addAndGet(estimatedTokens);
        log.info("generate: {} tokens, cumulative hourly spend {}/{}",
                estimatedTokens, cumulativeSpend, maxTokensPerHour);

        return response;
    }
}
