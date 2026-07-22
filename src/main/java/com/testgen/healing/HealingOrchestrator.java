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
import com.testgen.model.HealingResult;
import com.testgen.model.TestFailure;
import com.testgen.model.TestRun;
import com.testgen.persistence.DynamoDbTestRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.testgen.util.Constants.HEALING_STATUS_FAILED;
import static com.testgen.util.Constants.HEALING_STATUS_HEALED;
import static com.testgen.util.Constants.HEALING_STATUS_SKIPPED;
import static com.testgen.util.Constants.SCHEMA_VERSION_V1;
import static com.testgen.util.Constants.STATUS_HEALING_FAILED;
import static com.testgen.util.Constants.STATUS_HEALING_SUCCESS;

@Component
public class HealingOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(HealingOrchestrator.class);

    private final JUnitXmlReportParser xmlReportParser;
    private final DiffParser diffParser;
    private final DiffAnalyzer diffAnalyzer;
    private final ChangeCorrelator changeCorrelator;
    private final HealingTrigger healingTrigger;
    private final TestHealer testHealer;
    private final GitHubPrCreator gitHubPrCreator;
    private final DynamoDbTestRepository testRepository;

    public HealingOrchestrator(JUnitXmlReportParser xmlReportParser,
                               DiffParser diffParser,
                               DiffAnalyzer diffAnalyzer,
                               ChangeCorrelator changeCorrelator,
                               HealingTrigger healingTrigger,
                               TestHealer testHealer,
                               GitHubPrCreator gitHubPrCreator,
                               DynamoDbTestRepository testRepository) {
        this.xmlReportParser = xmlReportParser;
        this.diffParser = diffParser;
        this.diffAnalyzer = diffAnalyzer;
        this.changeCorrelator = changeCorrelator;
        this.healingTrigger = healingTrigger;
        this.testHealer = testHealer;
        this.gitHubPrCreator = gitHubPrCreator;
        this.testRepository = testRepository;
    }

    public HealingResponse orchestrate(HealingRequest request) {
        List<TestFailure> failures = xmlReportParser.parse(request.junitXmlReport());
        List<FileDiff> fileDiffs = diffParser.parse(request.diffContent());
        Map<String, String> sourceByPath = sourceByPath(fileDiffs, request.sourceFiles());
        List<ChangedMethod> changedMethods = diffAnalyzer.analyze(fileDiffs, sourceByPath);

        Map<TestFailure, List<ChangedMethod>> correlated = changeCorrelator.correlate(failures, changedMethods);

        List<HealedTestSummary> summaries = new ArrayList<>();
        for (Map.Entry<TestFailure, List<ChangedMethod>> entry : correlated.entrySet()) {
            summaries.add(healOne(request, entry.getKey(), entry.getValue()));
        }

        return new HealingResponse(request.testRunId(), List.copyOf(summaries), Instant.now());
    }

    private HealedTestSummary healOne(HealingRequest request, TestFailure failure, List<ChangedMethod> correlatedChanges) {
        String testClassName = simpleName(failure.testClassName());

        if (!healingTrigger.shouldHeal(failure, correlatedChanges)) {
            return new HealedTestSummary(testClassName, HEALING_STATUS_SKIPPED, null, List.of());
        }

        String classUnderTestClassName = correlatedChanges.getFirst().className();
        String failingTestCode = request.sourceFiles().get(testClassName);
        String classUnderTestSource = request.sourceFiles().get(classUnderTestClassName);

        if (failingTestCode == null || classUnderTestSource == null) {
            log.warn("Missing source for healing candidate {}: testSourcePresent={}, classUnderTestSourcePresent={}",
                    testClassName, failingTestCode != null, classUnderTestSource != null);
            return new HealedTestSummary(testClassName, HEALING_STATUS_SKIPPED, null,
                    List.of("Missing source in sourceFiles for " + testClassName + " or " + classUnderTestClassName));
        }

        HealingResult result = testHealer.heal(failingTestCode, classUnderTestSource, classUnderTestClassName, failure);

        return switch (result) {
            case HealingResult.HealingSuccess success -> deliver(request, testClassName, success);
            case HealingResult.HealingFailure failureResult -> {
                persistHealingRun(request, STATUS_HEALING_FAILED, null, null);
                yield new HealedTestSummary(testClassName, HEALING_STATUS_FAILED, null, failureResult.validationErrors());
            }
        };
    }

    private HealedTestSummary deliver(HealingRequest request, String testClassName, HealingResult.HealingSuccess success) {
        try {
            String testPrUrl = gitHubPrCreator.deliverHealedTest(new HealingDeliveryRequest(
                    request.owner(), request.repo(), request.sourceBranch(), request.sourceBranchSha(),
                    request.prNumber(), success.fixedTest(), testClassName));
            persistHealingRun(request, STATUS_HEALING_SUCCESS, success.fixedTest().testCode(), testPrUrl);
            return new HealedTestSummary(testClassName, HEALING_STATUS_HEALED, testPrUrl, List.of());
        } catch (Exception e) {
            log.error("Healed test {} passed validation but PR delivery to GitHub failed", testClassName, e);
            persistHealingRun(request, STATUS_HEALING_FAILED, null, null);
            return new HealedTestSummary(testClassName, HEALING_STATUS_FAILED, null,
                    List.of("Healed but PR delivery failed: " + e.getMessage()));
        }
    }

    private void persistHealingRun(HealingRequest request, String validationStatus, String generatedTestCode, String testPrUrl) {
        testRepository.save(new TestRun(
                UUID.randomUUID().toString(),
                request.owner() + "/" + request.repo(),
                String.valueOf(request.prNumber()),
                generatedTestCode,
                validationStatus,
                Instant.now().toString(),
                null,
                testPrUrl,
                SCHEMA_VERSION_V1));
    }

    private Map<String, String> sourceByPath(List<FileDiff> fileDiffs, Map<String, String> sourceFiles) {
        Map<String, String> result = new HashMap<>();
        for (FileDiff fileDiff : fileDiffs) {
            String source = sourceFiles.get(classNameFromPath(fileDiff.filePath()));
            if (source != null) {
                result.put(fileDiff.filePath(), source);
            }
        }
        return result;
    }

    private String classNameFromPath(String filePath) {
        int lastSeparator = Math.max(filePath.lastIndexOf('/'), filePath.lastIndexOf('\\'));
        String fileName = filePath.substring(lastSeparator + 1);
        return fileName.endsWith(".java") ? fileName.substring(0, fileName.length() - ".java".length()) : fileName;
    }

    private String simpleName(String fullyQualifiedClassName) {
        int lastDot = fullyQualifiedClassName.lastIndexOf('.');
        return lastDot == -1 ? fullyQualifiedClassName : fullyQualifiedClassName.substring(lastDot + 1);
    }
}
