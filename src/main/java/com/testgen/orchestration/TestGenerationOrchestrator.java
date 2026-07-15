package com.testgen.orchestration;

import com.testgen.analysis.DiffAnalyzer;
import com.testgen.analysis.DiffParser;
import com.testgen.api.TestGenerationRequest;
import com.testgen.api.TestGenerationResponse;
import com.testgen.context.ContextAssembler;
import com.testgen.generation.TestGenerationService;
import com.testgen.github.GitHubContentsFetcher;
import com.testgen.model.ChangedMethod;
import com.testgen.model.FileDiff;
import com.testgen.model.GeneratedTest;
import com.testgen.model.GenerationContext;
import com.testgen.model.TestRun;
import com.testgen.model.ValidationResult;
import com.testgen.persistence.DynamoDbTestRepository;
import com.testgen.persistence.S3TestArtifactStore;
import com.testgen.validation.TestValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.testgen.util.Constants.SCHEMA_VERSION_V1;
import static com.testgen.util.Constants.STATUS_COMPILE_FAILED;
import static com.testgen.util.Constants.STATUS_EXECUTION_FAILED;
import static com.testgen.util.Constants.STATUS_FAILED;
import static com.testgen.util.Constants.STATUS_SUCCESS;

@Component
public class TestGenerationOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(TestGenerationOrchestrator.class);

    private final GitHubContentsFetcher contentsFetcher;
    private final DiffParser diffParser;
    private final DiffAnalyzer diffAnalyzer;
    private final ContextAssembler contextAssembler;
    private final TestGenerationService testGenerationService;
    private final TestValidator testValidator;
    private final S3TestArtifactStore artifactStore;
    private final DynamoDbTestRepository testRepository;

    public TestGenerationOrchestrator(GitHubContentsFetcher contentsFetcher,
                                       DiffParser diffParser,
                                       DiffAnalyzer diffAnalyzer,
                                       ContextAssembler contextAssembler,
                                       TestGenerationService testGenerationService,
                                       TestValidator testValidator,
                                       S3TestArtifactStore artifactStore,
                                       DynamoDbTestRepository testRepository) {
        this.contentsFetcher = contentsFetcher;
        this.diffParser = diffParser;
        this.diffAnalyzer = diffAnalyzer;
        this.contextAssembler = contextAssembler;
        this.testGenerationService = testGenerationService;
        this.testValidator = testValidator;
        this.artifactStore = artifactStore;
        this.testRepository = testRepository;
    }

    public TestGenerationResponse orchestrate(TestGenerationRequest request) {
        String testRunId = UUID.randomUUID().toString();

        try {
            String fullSource = contentsFetcher
                    .fetchFileContent(request.owner(), request.repo(), request.changedFilePath(), request.ref())
                    .orElseThrow(() -> new IllegalStateException(
                            "Could not fetch source for " + request.changedFilePath() + " at ref " + request.ref()));

            List<FileDiff> fileDiffs = diffParser.parse(request.diffContent());
            List<ChangedMethod> changedMethods = diffAnalyzer.analyze(
                    fileDiffs, Map.of(request.changedFilePath(), fullSource));

            GenerationContext context = contextAssembler.assemble(
                    request.owner(), request.repo(), request.ref(), request.changedFilePath(),
                    changedMethods, request.repositoryId());

            GeneratedTest generatedTest = testGenerationService.generate(context);
            ValidationResult validationResult = testValidator.validate(generatedTest);

            String s3Key = S3TestArtifactStore.buildKey(
                    request.repositoryId(), testRunId, generatedTest.className());
            String s3Uri = artifactStore.upload(generatedTest.testCode(), s3Key);

            return persistAndRespond(testRunId, request, generatedTest, validationResult, s3Uri);
        } catch (Exception e) {
            log.error("Test generation failed for testRunId={}, repositoryId={}, pullRequestId={}",
                    testRunId, request.repositoryId(), request.pullRequestId(), e);

            testRepository.save(new TestRun(
                    testRunId, request.repositoryId(), request.pullRequestId(),
                    null, STATUS_FAILED, Instant.now().toString(), null, SCHEMA_VERSION_V1));

            return new TestGenerationResponse(
                    testRunId, null, STATUS_FAILED, List.of(), null, null, Instant.now());
        }
    }

    private TestGenerationResponse persistAndRespond(String testRunId, TestGenerationRequest request,
                                                       GeneratedTest generatedTest,
                                                       ValidationResult validationResult, String s3Uri) {
        String status = statusFor(validationResult);
        List<String> errors = errorsFor(validationResult);

        testRepository.save(new TestRun(
                testRunId, request.repositoryId(), request.pullRequestId(),
                generatedTest.testCode(), status, Instant.now().toString(), s3Uri, SCHEMA_VERSION_V1));

        return new TestGenerationResponse(
                testRunId, generatedTest.testCode(), status, errors, s3Uri, null, Instant.now());
    }

    private String statusFor(ValidationResult result) {
        return switch (result) {
            case ValidationResult.ValidationSuccess ignored -> STATUS_SUCCESS;
            case ValidationResult.ValidationFailure failure -> switch (failure.failedAt()) {
                case COMPILE -> STATUS_COMPILE_FAILED;
                case EXECUTE -> STATUS_EXECUTION_FAILED;
            };
        };
    }

    private List<String> errorsFor(ValidationResult result) {
        return switch (result) {
            case ValidationResult.ValidationSuccess ignored -> List.of();
            case ValidationResult.ValidationFailure failure -> failure.errors();
        };
    }
}
