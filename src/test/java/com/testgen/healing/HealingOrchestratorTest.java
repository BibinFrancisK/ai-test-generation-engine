package com.testgen.healing;

import com.testgen.analysis.DiffAnalyzer;
import com.testgen.analysis.DiffParser;
import com.testgen.api.HealedTestSummary;
import com.testgen.api.HealingRequest;
import com.testgen.api.HealingResponse;
import com.testgen.github.GitHubPrCreator;
import com.testgen.github.HealingDeliveryRequest;
import com.testgen.model.ChangedMethod;
import com.testgen.model.FileDiff;
import com.testgen.model.GeneratedTest;
import com.testgen.model.HealingResult;
import com.testgen.model.TestFailure;
import com.testgen.model.TestRun;
import com.testgen.persistence.DynamoDbTestRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static com.testgen.util.Constants.STATUS_HEALING_FAILED;
import static com.testgen.util.Constants.STATUS_HEALING_SUCCESS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HealingOrchestratorTest {

    private static final String OWNER = "owner";
    private static final String REPO = "repo";
    private static final int PR_NUMBER = 7;
    private static final String REPOSITORY_ID = OWNER + "/" + REPO;
    private static final String TEST_CLASS = "com.example.service.SampleServiceTest";
    private static final String SOURCE_CLASS = "SampleService";

    @Mock
    private JUnitXmlReportParser xmlReportParser;
    @Mock
    private DiffParser diffParser;
    @Mock
    private DiffAnalyzer diffAnalyzer;
    @Mock
    private ChangeCorrelator changeCorrelator;
    @Mock
    private HealingTrigger healingTrigger;
    @Mock
    private TestHealer testHealer;
    @Mock
    private GitHubPrCreator gitHubPrCreator;
    @Mock
    private DynamoDbTestRepository testRepository;

    private HealingOrchestrator orchestrator;

    private HealingOrchestrator newOrchestrator() {
        return new HealingOrchestrator(xmlReportParser, diffParser, diffAnalyzer, changeCorrelator,
                healingTrigger, testHealer, gitHubPrCreator, testRepository);
    }

    private HealingRequest request() {
        return new HealingRequest("testRunId", "main", "sha123", OWNER, REPO, PR_NUMBER,
                "<xml/>", "diff content",
                Map.of(
                        "SampleServiceTest", "class SampleServiceTest {}",
                        "SampleService", "class SampleService {}"));
    }

    private TestFailure failure() {
        return new TestFailure(TEST_CLASS, "someTest", "boom", "trace");
    }

    private ChangedMethod changedMethod() {
        return new ChangedMethod(SOURCE_CLASS, "getStatus", List.of(), "String", List.of(), 1, 5);
    }

    private void stubPipeline(TestFailure failure, List<ChangedMethod> correlatedChanges) {
        orchestrator = newOrchestrator();
        when(xmlReportParser.parse(any())).thenReturn(List.of(failure));
        when(diffParser.parse(any())).thenReturn(List.of());
        when(diffAnalyzer.analyze(any(), any())).thenReturn(List.of());
        when(changeCorrelator.correlate(any(), any())).thenReturn(Map.of(failure, correlatedChanges));
    }

    @Test
    void persistsHealingSuccessAfterDelivery() {
        TestFailure failure = failure();
        stubPipeline(failure, List.of(changedMethod()));
        when(healingTrigger.shouldHeal(any(), any())).thenReturn(true);

        GeneratedTest fixedTest = new GeneratedTest("SampleServiceTest", "com.example.service",
                "class SampleServiceTest { /* fixed */ }", null, Instant.now());
        when(testHealer.heal(any(), any(), any(), any()))
                .thenReturn(new HealingResult.HealingSuccess(fixedTest, "Fixed on first attempt"));
        when(gitHubPrCreator.deliverHealedTest(any(HealingDeliveryRequest.class)))
                .thenReturn("https://github.com/owner/repo/pull/99");

        HealingResponse response = orchestrator.orchestrate(request());

        assertThat(response.results()).hasSize(1);
        HealedTestSummary summary = response.results().getFirst();
        assertThat(summary.status()).isEqualTo("HEALED");
        assertThat(summary.testPrUrl()).isEqualTo("https://github.com/owner/repo/pull/99");

        verify(testRepository).save(argThat((TestRun run) ->
                run.repositoryId().equals(REPOSITORY_ID)
                        && run.pullRequestId().equals(String.valueOf(PR_NUMBER))
                        && run.validationStatus().equals(STATUS_HEALING_SUCCESS)
                        && run.generatedTestCode().equals(fixedTest.testCode())
                        && run.testPrUrl().equals("https://github.com/owner/repo/pull/99")));
    }

    @Test
    void persistsHealingFailureWhenValidationFailsAfterRetry() {
        TestFailure failure = failure();
        stubPipeline(failure, List.of(changedMethod()));
        when(healingTrigger.shouldHeal(any(), any())).thenReturn(true);
        when(testHealer.heal(any(), any(), any(), any()))
                .thenReturn(new HealingResult.HealingFailure("Validation failed after retry", List.of("compile error")));

        HealingResponse response = orchestrator.orchestrate(request());

        assertThat(response.results()).hasSize(1);
        assertThat(response.results().getFirst().status()).isEqualTo("FAILED");

        verify(testRepository).save(argThat((TestRun run) ->
                run.repositoryId().equals(REPOSITORY_ID)
                        && run.pullRequestId().equals(String.valueOf(PR_NUMBER))
                        && run.validationStatus().equals(STATUS_HEALING_FAILED)
                        && run.generatedTestCode() == null
                        && run.testPrUrl() == null));
        verify(gitHubPrCreator, never()).deliverHealedTest(any());
    }

    @Test
    void doesNotPersistWhenHealingIsSkipped() {
        TestFailure failure = failure();
        stubPipeline(failure, List.of());
        when(healingTrigger.shouldHeal(any(), any())).thenReturn(false);

        HealingResponse response = orchestrator.orchestrate(request());

        assertThat(response.results()).hasSize(1);
        assertThat(response.results().getFirst().status()).isEqualTo("SKIPPED");

        verify(testRepository, never()).save(any());
        verify(testHealer, never()).heal(any(), any(), any(), any());
    }
}
