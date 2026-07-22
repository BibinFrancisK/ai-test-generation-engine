package com.testgen.dashboard;

import com.testgen.model.CoverageStats;
import com.testgen.model.TestRun;
import com.testgen.persistence.DynamoDbTestRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CoverageAggregatorTest {

    private static final String REPOSITORY_ID = "owner/repo";

    @Mock
    private DynamoDbTestRepository testRepository;

    private CoverageAggregator aggregator() {
        return new CoverageAggregator(testRepository);
    }

    private TestRun run(String validationStatus) {
        return new TestRun("run-" + validationStatus + "-" + Math.random(), REPOSITORY_ID, "pr-1",
                null, validationStatus, Instant.now().toString(), null, null, "v1");
    }

    @Test
    void returnsAllZeroStatsWhenNoRunsExist() {
        when(testRepository.findByRepositoryId(REPOSITORY_ID)).thenReturn(List.of());

        CoverageStats stats = aggregator().aggregate(REPOSITORY_ID);

        assertThat(stats).isEqualTo(new CoverageStats(0, 0, 0, 0, 0, 0, 0.0));
    }

    @Test
    void aggregatesGenerationFlowStatusesCorrectly() {
        when(testRepository.findByRepositoryId(REPOSITORY_ID)).thenReturn(List.of(
                run("SUCCESS"),
                run("SUCCESS"),
                run("COMPILE_FAILED"),
                run("EXECUTION_FAILED"),
                run("FAILED")
        ));

        CoverageStats stats = aggregator().aggregate(REPOSITORY_ID);

        assertThat(stats.totalTestRuns()).isEqualTo(5);
        assertThat(stats.aiGeneratedTests()).isEqualTo(4);
        assertThat(stats.compilePassed()).isEqualTo(3);
        assertThat(stats.executionPassed()).isEqualTo(2);
        assertThat(stats.passRate()).isEqualTo(3.0 / 5);
    }

    @Test
    void aggregatesHealingStatusesCorrectly() {
        when(testRepository.findByRepositoryId(REPOSITORY_ID)).thenReturn(List.of(
                run("HEALING_SUCCESS"),
                run("HEALING_SUCCESS"),
                run("HEALING_FAILED")
        ));

        CoverageStats stats = aggregator().aggregate(REPOSITORY_ID);

        assertThat(stats.selfHealingAttempts()).isEqualTo(3);
        assertThat(stats.selfHealingSuccesses()).isEqualTo(2);
    }

    @Test
    void excludesHealingRunsFromGenerationCounts() {
        when(testRepository.findByRepositoryId(REPOSITORY_ID)).thenReturn(List.of(
                run("SUCCESS"),
                run("HEALING_SUCCESS"),
                run("HEALING_FAILED")
        ));

        CoverageStats stats = aggregator().aggregate(REPOSITORY_ID);

        assertThat(stats.totalTestRuns()).isEqualTo(1);
        assertThat(stats.aiGeneratedTests()).isEqualTo(1);
        assertThat(stats.compilePassed()).isEqualTo(1);
        assertThat(stats.executionPassed()).isEqualTo(1);
        assertThat(stats.selfHealingAttempts()).isEqualTo(2);
        assertThat(stats.selfHealingSuccesses()).isEqualTo(1);
        assertThat(stats.passRate()).isEqualTo(1.0);
    }
}
