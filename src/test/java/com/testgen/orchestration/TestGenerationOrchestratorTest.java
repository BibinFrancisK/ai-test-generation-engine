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
import com.testgen.model.ProjectConventions;
import com.testgen.model.TestRun;
import com.testgen.model.ValidationResult;
import com.testgen.model.ValidationStage;
import com.testgen.persistence.DynamoDbTestRepository;
import com.testgen.persistence.S3TestArtifactStore;
import com.testgen.validation.TestValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TestGenerationOrchestratorTest {

    private static final String OWNER = "owner";
    private static final String REPO = "repo";
    private static final String REF = "main";
    private static final String CHANGED_FILE_PATH = "src/main/java/com/example/Foo.java";
    private static final String REPOSITORY_ID = "repo-123";
    private static final String PULL_REQUEST_ID = "pr-1";
    private static final String DIFF_CONTENT = "diff content";

    @Mock
    private GitHubContentsFetcher contentsFetcher;
    @Mock
    private DiffParser diffParser;
    @Mock
    private DiffAnalyzer diffAnalyzer;
    @Mock
    private ContextAssembler contextAssembler;
    @Mock
    private TestGenerationService testGenerationService;
    @Mock
    private TestValidator testValidator;
    @Mock
    private S3TestArtifactStore artifactStore;
    @Mock
    private DynamoDbTestRepository testRepository;

    private TestGenerationOrchestrator orchestrator;
    private TestGenerationRequest request;

    @BeforeEach
    void setUp() {
        orchestrator = new TestGenerationOrchestrator(contentsFetcher, diffParser, diffAnalyzer, contextAssembler,
                testGenerationService, testValidator, artifactStore, testRepository);
        request = new TestGenerationRequest(OWNER, REPO, REF, REPOSITORY_ID, PULL_REQUEST_ID, DIFF_CONTENT, CHANGED_FILE_PATH);
    }

    @Test
    void happyPathRunsCollaboratorsInOrderAndPersistsSuccessfulTestRun() {
        List<ChangedMethod> changedMethods = List.of(sampleMethod());
        List<FileDiff> fileDiffs = List.of();
        GenerationContext context = sampleContext(changedMethods);
        GeneratedTest generatedTest = new GeneratedTest(
                "FooTest", "com.example", "class FooTest {}", Path.of("tmp", "FooTest.java"), Instant.now());
        ValidationResult.ValidationSuccess success =
                new ValidationResult.ValidationSuccess("FooTest", List.of("shouldWork"), 1);

        when(contentsFetcher.fetchFileContent(OWNER, REPO, CHANGED_FILE_PATH, REF))
                .thenReturn(Optional.of("full source"));
        when(diffParser.parse(DIFF_CONTENT)).thenReturn(fileDiffs);
        when(diffAnalyzer.analyze(fileDiffs, Map.of(CHANGED_FILE_PATH, "full source"))).thenReturn(changedMethods);
        when(contextAssembler.assemble(OWNER, REPO, REF, CHANGED_FILE_PATH, changedMethods, REPOSITORY_ID))
                .thenReturn(context);
        when(testGenerationService.generate(context)).thenReturn(generatedTest);
        when(testValidator.validate(generatedTest)).thenReturn(success);
        when(artifactStore.upload(eq("class FooTest {}"), anyString())).thenReturn("s3://bucket/key");

        TestGenerationResponse response = orchestrator.orchestrate(request);

        InOrder inOrder = inOrder(contentsFetcher, diffParser, diffAnalyzer, contextAssembler,
                testGenerationService, testValidator, artifactStore, testRepository);
        inOrder.verify(contentsFetcher).fetchFileContent(OWNER, REPO, CHANGED_FILE_PATH, REF);
        inOrder.verify(diffParser).parse(DIFF_CONTENT);
        inOrder.verify(diffAnalyzer).analyze(fileDiffs, Map.of(CHANGED_FILE_PATH, "full source"));
        inOrder.verify(contextAssembler).assemble(OWNER, REPO, REF, CHANGED_FILE_PATH, changedMethods, REPOSITORY_ID);
        inOrder.verify(testGenerationService).generate(context);
        inOrder.verify(testValidator).validate(generatedTest);
        inOrder.verify(artifactStore).upload(eq("class FooTest {}"), anyString());
        inOrder.verify(testRepository).save(any(TestRun.class));

        assertThat(response.testRunId()).isNotBlank();
        assertThat(response.validationStatus()).isEqualTo("SUCCESS");
        assertThat(response.generatedTestCode()).isEqualTo("class FooTest {}");
        assertThat(response.s3ArtifactUrl()).isEqualTo("s3://bucket/key");
        assertThat(response.validationErrors()).isEmpty();
    }

    @Test
    void compileFailureStillPersistsTestRunAndReturns422ShapedResponse() {
        List<ChangedMethod> changedMethods = List.of(sampleMethod());
        GenerationContext context = sampleContext(changedMethods);
        GeneratedTest generatedTest = new GeneratedTest(
                "FooTest", "com.example", "class FooTest {}", Path.of("tmp", "FooTest.java"), Instant.now());
        ValidationResult.ValidationFailure failure = new ValidationResult.ValidationFailure(
                "FooTest", List.of("cannot find symbol"), ValidationStage.COMPILE);

        when(contentsFetcher.fetchFileContent(OWNER, REPO, CHANGED_FILE_PATH, REF))
                .thenReturn(Optional.of("full source"));
        when(diffParser.parse(DIFF_CONTENT)).thenReturn(List.of());
        when(diffAnalyzer.analyze(List.of(), Map.of(CHANGED_FILE_PATH, "full source"))).thenReturn(changedMethods);
        when(contextAssembler.assemble(OWNER, REPO, REF, CHANGED_FILE_PATH, changedMethods, REPOSITORY_ID))
                .thenReturn(context);
        when(testGenerationService.generate(context)).thenReturn(generatedTest);
        when(testValidator.validate(generatedTest)).thenReturn(failure);
        when(artifactStore.upload(anyString(), anyString())).thenReturn("s3://bucket/key");

        TestGenerationResponse response = orchestrator.orchestrate(request);

        assertThat(response.validationStatus()).isEqualTo("COMPILE_FAILED");
        assertThat(response.validationErrors()).containsExactly("cannot find symbol");
        verify(testRepository).save(any(TestRun.class));
    }

    @Test
    void sourceFetchFailurePersistsFailedTestRunAndDoesNotThrow() {
        when(contentsFetcher.fetchFileContent(OWNER, REPO, CHANGED_FILE_PATH, REF))
                .thenReturn(Optional.empty());

        TestGenerationResponse response = orchestrator.orchestrate(request);

        assertThat(response.testRunId()).isNotBlank();
        assertThat(response.validationStatus()).isEqualTo("FAILED");
        assertThat(response.generatedTestCode()).isNull();
        verify(testRepository).save(any(TestRun.class));
        verify(artifactStore, never()).upload(anyString(), anyString());
    }

    private ChangedMethod sampleMethod() {
        return new ChangedMethod("Foo", "bar", List.of(), "void", List.of(), 1, 5);
    }

    private GenerationContext sampleContext(List<ChangedMethod> changedMethods) {
        ProjectConventions conventions = new ProjectConventions(
                REPOSITORY_ID, "v1", "junit5", "mockito", Optional.empty(), "mirrors-source", Instant.now());
        return new GenerationContext("full source", Optional.empty(), List.of(), conventions, changedMethods);
    }
}
