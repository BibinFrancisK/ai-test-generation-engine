package com.testgen.generation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class LlmSpendGuardTest {

    // "system" (6 chars) + "user" (4 chars) = 10 chars -> 10/4 = 2 estimated tokens per call
    private static final String SYSTEM_PROMPT = "system";
    private static final String USER_PROMPT = "user";

    // Spy on a concrete permitted type — Mockito cannot mock sealed interfaces directly
    @Spy
    private NoopProvider delegate;

    @Test
    void underBudgetCallPassesThroughToDelegate() {
        doReturn("generated test").when(delegate).generate(SYSTEM_PROMPT, USER_PROMPT);
        LlmSpendGuard guard = new LlmSpendGuard(delegate, 50_000, fixedClock());

        String result = guard.generate(SYSTEM_PROMPT, USER_PROMPT);

        assertThat(result).isEqualTo("generated test");
        verify(delegate).generate(SYSTEM_PROMPT, USER_PROMPT);
    }

    @Test
    void callExceedingHourlyBudgetThrowsAndDoesNotCallDelegate() {
        LlmSpendGuard guard = new LlmSpendGuard(delegate, 1, fixedClock());

        assertThatThrownBy(() -> guard.generate(SYSTEM_PROMPT, USER_PROMPT))
                .isInstanceOf(SpendLimitExceededException.class);

        verify(delegate, never()).generate(any(), any());
    }

    @Test
    void secondCallInSameHourAccumulatesAgainstTheSameBudget() {
        doReturn("generated test").when(delegate).generate(any(), any());
        // Budget covers exactly one 2-token call; the second call in the same hour must be rejected.
        LlmSpendGuard guard = new LlmSpendGuard(delegate, 2, fixedClock());

        guard.generate(SYSTEM_PROMPT, USER_PROMPT);

        assertThatThrownBy(() -> guard.generate(SYSTEM_PROMPT, USER_PROMPT))
                .isInstanceOf(SpendLimitExceededException.class);
        verify(delegate).generate(SYSTEM_PROMPT, USER_PROMPT);
    }

    @Test
    void hourBoundaryRolloverResetsTheCounter() {
        doReturn("generated test").when(delegate).generate(any(), any());
        Instant firstHour = Instant.parse("2026-07-27T10:30:00Z");
        Instant secondHour = Instant.parse("2026-07-27T11:00:01Z");
        Clock clock = tickingClock(firstHour, secondHour);
        // Budget covers exactly one 2-token call per hour.
        LlmSpendGuard guard = new LlmSpendGuard(delegate, 2, clock);

        guard.generate(SYSTEM_PROMPT, USER_PROMPT);
        String secondCallResult = guard.generate(SYSTEM_PROMPT, USER_PROMPT);

        assertThat(secondCallResult).isEqualTo("generated test");
        verify(delegate, times(2)).generate(SYSTEM_PROMPT, USER_PROMPT);
    }

    private static Clock fixedClock() {
        return Clock.fixed(Instant.parse("2026-07-27T10:30:00Z"), ZoneOffset.UTC);
    }

    private static Clock tickingClock(Instant... instants) {
        return new Clock() {
            private int index = 0;

            @Override
            public ZoneId getZone() {
                return ZoneOffset.UTC;
            }

            @Override
            public Clock withZone(ZoneId zone) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Instant instant() {
                Instant current = instants[Math.min(index, instants.length - 1)];
                index++;
                return current;
            }
        };
    }
}
