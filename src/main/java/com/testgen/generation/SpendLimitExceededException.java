package com.testgen.generation;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class SpendLimitExceededException extends ResponseStatusException {

    public SpendLimitExceededException(int attemptedTokens, int maxTokensPerHour) {
        super(HttpStatus.TOO_MANY_REQUESTS,
                "Hourly LLM token budget exceeded: attempted %d tokens, budget %d tokens/hour"
                        .formatted(attemptedTokens, maxTokensPerHour));
    }
}
