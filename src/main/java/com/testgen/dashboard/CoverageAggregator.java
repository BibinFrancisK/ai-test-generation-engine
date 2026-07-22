package com.testgen.dashboard;

import com.testgen.model.CoverageStats;
import com.testgen.model.TestRun;
import com.testgen.persistence.DynamoDbTestRepository;
import org.springframework.stereotype.Component;

import java.util.List;

import static com.testgen.util.Constants.STATUS_COMPILE_FAILED;
import static com.testgen.util.Constants.STATUS_EXECUTION_FAILED;
import static com.testgen.util.Constants.STATUS_FAILED;
import static com.testgen.util.Constants.STATUS_HEALING_FAILED;
import static com.testgen.util.Constants.STATUS_HEALING_SUCCESS;
import static com.testgen.util.Constants.STATUS_SUCCESS;

@Component
public class CoverageAggregator {

    private final DynamoDbTestRepository testRepository;

    public CoverageAggregator(DynamoDbTestRepository testRepository) {
        this.testRepository = testRepository;
    }

    public CoverageStats aggregate(String repositoryId) {
        List<TestRun> runs = testRepository.findByRepositoryId(repositoryId);

        long totalTestRuns = 0;
        long aiGeneratedTests = 0;
        long compilePassed = 0;
        long executionPassed = 0;
        long selfHealingAttempts = 0;
        long selfHealingSuccesses = 0;

        for (TestRun run : runs) {
            switch (run.validationStatus()) {
                case STATUS_SUCCESS -> {
                    totalTestRuns++;
                    aiGeneratedTests++;
                    compilePassed++;
                    executionPassed++;
                }
                case STATUS_EXECUTION_FAILED -> {
                    totalTestRuns++;
                    aiGeneratedTests++;
                    compilePassed++;
                }
                case STATUS_COMPILE_FAILED -> {
                    totalTestRuns++;
                    aiGeneratedTests++;
                }
                case STATUS_FAILED -> totalTestRuns++;
                case STATUS_HEALING_SUCCESS -> {
                    selfHealingAttempts++;
                    selfHealingSuccesses++;
                }
                case STATUS_HEALING_FAILED -> selfHealingAttempts++;
                default -> {
                    // Unrecognized status — ignore rather than mis-attribute it to a bucket.
                }
            }
        }

        double passRate = totalTestRuns == 0 ? 0.0 : (double) compilePassed / totalTestRuns;

        return new CoverageStats(totalTestRuns, aiGeneratedTests, compilePassed, executionPassed,
                selfHealingAttempts, selfHealingSuccesses, passRate);
    }
}
