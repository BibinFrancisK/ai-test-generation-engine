package com.testgen.context;

import com.testgen.analysis.ProjectConventionsAnalyzer;
import com.testgen.github.GitHubContentsFetcher;
import com.testgen.model.ChangedMethod;
import com.testgen.model.GenerationContext;
import com.testgen.model.ProjectConventions;
import com.testgen.persistence.ProjectConventionsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static com.testgen.util.Constants.*;

@Component
public class ContextAssembler {

    private static final Logger log = LoggerFactory.getLogger(ContextAssembler.class);

    private final GitHubContentsFetcher contentsFetcher;
    private final ProjectConventionsRepository conventionsRepository;
    private final ProjectConventionsAnalyzer conventionsAnalyzer;

    public ContextAssembler(GitHubContentsFetcher contentsFetcher,
                             ProjectConventionsRepository conventionsRepository,
                             ProjectConventionsAnalyzer conventionsAnalyzer) {
        this.contentsFetcher = contentsFetcher;
        this.conventionsRepository = conventionsRepository;
        this.conventionsAnalyzer = conventionsAnalyzer;
    }

    public GenerationContext assemble(String owner, String repo, String ref, String changedFilePath,
                                       List<ChangedMethod> changedMethods, String repositoryId) {
        String fullSource = contentsFetcher.fetchFileContent(owner, repo, changedFilePath, ref)
                .orElseThrow(() -> new IllegalStateException(
                        "Could not fetch source for " + changedFilePath + " at ref " + ref));
        Optional<String> existingTestSource = contentsFetcher.findTestFile(owner, repo, changedFilePath, ref);
        List<String> dependencySources = contentsFetcher.fetchDependencySources(
                owner, repo, fullSource, ref, MAX_DEPENDENCY_FILES);

        ProjectConventions conventions = resolveConventions(owner, repo, ref, repositoryId);

        return new GenerationContext(fullSource, existingTestSource, dependencySources, conventions, changedMethods);
    }

    private ProjectConventions resolveConventions(String owner, String repo, String ref, String repositoryId) {
        Optional<ProjectConventions> cached = conventionsRepository.findByRepositoryId(repositoryId);
        if (cached.isPresent() && isFresh(cached.get())) {
            log.debug("Using cached conventions for repository {}", repositoryId);
            return cached.get();
        }

        log.debug("Conventions cache miss or stale for repository {}; re-analyzing", repositoryId);
        List<String> testFilePaths = contentsFetcher.listDirectoryJavaFiles(
                owner, repo, TEST_SOURCE_ROOT, ref, MAX_CONVENTION_TEST_FILES);
        List<String> testSources = testFilePaths.stream()
                .map(path -> contentsFetcher.fetchFileContent(owner, repo, path, ref))
                .flatMap(Optional::stream)
                .toList();

        ProjectConventions analyzed = conventionsAnalyzer.analyze(repositoryId, testSources);
        conventionsRepository.save(analyzed);
        return analyzed;
    }

    private boolean isFresh(ProjectConventions conventions) {
        return Duration.between(conventions.analyzedAt(), Instant.now()).compareTo(CONVENTIONS_TTL) <= 0;
    }
}
