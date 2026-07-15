package com.testgen.context;

import com.testgen.analysis.ProjectConventionsAnalyzer;
import com.testgen.github.GitHubContentsFetcher;
import com.testgen.model.ChangedMethod;
import com.testgen.model.GenerationContext;
import com.testgen.model.ProjectConventions;
import com.testgen.persistence.ProjectConventionsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContextAssemblerTest {

    private static final String OWNER = "owner";
    private static final String REPO = "repo";
    private static final String REF = "main";
    private static final String CHANGED_FILE_PATH = "src/main/java/com/example/Foo.java";
    private static final String REPOSITORY_ID = "repo-123";
    private static final String TEST_SOURCE_ROOT = "src/test/java";

    @Mock
    private GitHubContentsFetcher contentsFetcher;

    @Mock
    private ProjectConventionsRepository conventionsRepository;

    @Mock
    private ProjectConventionsAnalyzer conventionsAnalyzer;

    private ContextAssembler assembler;
    private List<ChangedMethod> changedMethods;

    @BeforeEach
    void setUp() {
        assembler = new ContextAssembler(contentsFetcher, conventionsRepository, conventionsAnalyzer);
        changedMethods = List.of(new ChangedMethod(
                "Foo", "bar", List.of(), "void", List.of(), 1, 5));
    }

    @Test
    void tier1FetchesRunInOrderAndPopulateGenerationContext() {
        stubTier1();
        ProjectConventions fresh = freshConventions();
        when(conventionsRepository.findByRepositoryId(REPOSITORY_ID)).thenReturn(Optional.of(fresh));

        GenerationContext result = assembler.assemble(OWNER, REPO, REF, CHANGED_FILE_PATH, changedMethods, REPOSITORY_ID);

        InOrder inOrder = inOrder(contentsFetcher);
        inOrder.verify(contentsFetcher).fetchFileContent(OWNER, REPO, CHANGED_FILE_PATH, REF);
        inOrder.verify(contentsFetcher).findTestFile(OWNER, REPO, CHANGED_FILE_PATH, REF);
        inOrder.verify(contentsFetcher).fetchDependencySources(OWNER, REPO, "full source", REF, 3);

        assertThat(result.fullSourceCode()).isEqualTo("full source");
        assertThat(result.existingTestSource()).contains("existing test");
        assertThat(result.dependencySources()).isEmpty();
        assertThat(result.conventions()).isEqualTo(fresh);
        assertThat(result.changedMethods()).isEqualTo(changedMethods);
    }

    @Test
    void freshCachedConventionsSkipsAnalyzer() {
        stubTier1();
        ProjectConventions fresh = freshConventions();
        when(conventionsRepository.findByRepositoryId(REPOSITORY_ID)).thenReturn(Optional.of(fresh));

        assembler.assemble(OWNER, REPO, REF, CHANGED_FILE_PATH, changedMethods, REPOSITORY_ID);

        verifyNoInteractions(conventionsAnalyzer);
        verify(conventionsRepository, never()).save(any());
        verify(contentsFetcher, never()).listDirectoryJavaFiles(anyString(), anyString(), anyString(), anyString(), anyInt());
    }

    @Test
    void staleCachedConventionsTriggersReanalysisAndSave() {
        stubTier1();
        when(conventionsRepository.findByRepositoryId(REPOSITORY_ID)).thenReturn(Optional.of(staleConventions()));
        when(contentsFetcher.listDirectoryJavaFiles(OWNER, REPO, TEST_SOURCE_ROOT, REF, 5))
                .thenReturn(List.of("src/test/java/com/example/FooTest.java"));
        when(contentsFetcher.fetchFileContent(OWNER, REPO, "src/test/java/com/example/FooTest.java", REF))
                .thenReturn(Optional.of("test source"));
        ProjectConventions reanalyzed = freshConventions();
        when(conventionsAnalyzer.analyze(REPOSITORY_ID, List.of("test source"))).thenReturn(reanalyzed);

        GenerationContext result = assembler.assemble(OWNER, REPO, REF, CHANGED_FILE_PATH, changedMethods, REPOSITORY_ID);

        verify(conventionsAnalyzer).analyze(REPOSITORY_ID, List.of("test source"));
        verify(conventionsRepository).save(reanalyzed);
        assertThat(result.conventions()).isEqualTo(reanalyzed);
    }

    @Test
    void missingCachedConventionsTriggersAnalysis() {
        stubTier1();
        when(conventionsRepository.findByRepositoryId(REPOSITORY_ID)).thenReturn(Optional.empty());
        when(contentsFetcher.listDirectoryJavaFiles(OWNER, REPO, TEST_SOURCE_ROOT, REF, 5))
                .thenReturn(List.of());
        ProjectConventions analyzed = freshConventions();
        when(conventionsAnalyzer.analyze(REPOSITORY_ID, List.of())).thenReturn(analyzed);

        assembler.assemble(OWNER, REPO, REF, CHANGED_FILE_PATH, changedMethods, REPOSITORY_ID);

        verify(conventionsAnalyzer).analyze(REPOSITORY_ID, List.of());
        verify(conventionsRepository).save(analyzed);
    }

    private void stubTier1() {
        when(contentsFetcher.fetchFileContent(OWNER, REPO, CHANGED_FILE_PATH, REF))
                .thenReturn(Optional.of("full source"));
        when(contentsFetcher.findTestFile(OWNER, REPO, CHANGED_FILE_PATH, REF))
                .thenReturn(Optional.of("existing test"));
        when(contentsFetcher.fetchDependencySources(OWNER, REPO, "full source", REF, 3))
                .thenReturn(List.of());
    }

    private ProjectConventions freshConventions() {
        return new ProjectConventions(REPOSITORY_ID, "v1", "junit5", "mockito",
                Optional.empty(), "mirrors-source", Instant.now());
    }

    private ProjectConventions staleConventions() {
        return new ProjectConventions(REPOSITORY_ID, "v1", "junit5", "mockito",
                Optional.empty(), "mirrors-source", Instant.now().minus(8, ChronoUnit.DAYS));
    }
}
