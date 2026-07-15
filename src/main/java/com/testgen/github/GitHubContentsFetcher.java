package com.testgen.github;

import com.testgen.model.ContentsApiResponse;
import com.testgen.model.ContentsListEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;

import static com.testgen.util.Constants.IMPORT_PATTERN;
import static com.testgen.util.Constants.SKIPPED_IMPORT_PREFIXES;

@Component
public class GitHubContentsFetcher {

    private static final Logger log = LoggerFactory.getLogger(GitHubContentsFetcher.class);

    private final RestClient gitHubRestClient;

    public GitHubContentsFetcher(RestClient gitHubRestClient) {
        this.gitHubRestClient = gitHubRestClient;
    }

    public Optional<String> findTestFile(String owner, String repo, String sourceFilePath, String ref) {
        String testFilePath = toTestFilePath(sourceFilePath);
        return fetchFileContent(owner, repo, testFilePath, ref);
    }

    public Optional<String> fetchFileContent(String owner, String repo, String path, String ref) {
        try {
            ContentsApiResponse response = gitHubRestClient.get()
                    .uri("/repos/{owner}/{repo}/contents/" + path + "?ref={ref}", owner, repo, ref)
                    .retrieve()
                    .body(ContentsApiResponse.class);

            if (response == null || response.content() == null) {
                return Optional.empty();
            }

            byte[] decoded = Base64.getMimeDecoder().decode(response.content());
            return Optional.of(new String(decoded, StandardCharsets.UTF_8));
        } catch (HttpClientErrorException.NotFound e) {
            return Optional.empty();
        }
    }

    public List<String> listDirectoryJavaFiles(String owner, String repo, String directoryPath, String ref, int maxFiles) {
        try {
            ContentsListEntry[] entries = gitHubRestClient.get()
                    .uri("/repos/{owner}/{repo}/contents/" + directoryPath + "?ref={ref}", owner, repo, ref)
                    .retrieve()
                    .body(ContentsListEntry[].class);

            if (entries == null) {
                return List.of();
            }

            return Arrays.stream(entries)
                    .filter(entry -> "file".equals(entry.type()) && entry.name().endsWith(".java"))
                    .map(ContentsListEntry::path)
                    .limit(maxFiles)
                    .toList();
        } catch (HttpClientErrorException.NotFound e) {
            return List.of();
        }
    }

    public List<String> fetchDependencySources(String owner, String repo, String sourceContent, String ref, int maxFiles) {
        List<String> dependencyPaths = extractLocalImports(sourceContent, maxFiles);

        List<String> sources = new ArrayList<>();
        for (String path : dependencyPaths) {
            fetchFileContent(owner, repo, path, ref).ifPresent(sources::add);
        }
        return List.copyOf(sources);
    }

    private String toTestFilePath(String sourceFilePath) {
        return sourceFilePath
                .replaceFirst("^src/main/java/", "src/test/java/")
                .replaceFirst("\\.java$", "Test.java");
    }

    private List<String> extractLocalImports(String sourceContent, int maxFiles) {
        List<String> paths = new ArrayList<>();
        Matcher matcher = IMPORT_PATTERN.matcher(sourceContent);

        while (matcher.find() && paths.size() < maxFiles) {
            String importedClass = matcher.group(1);
            if (isProjectLocal(importedClass)) {
                paths.add("src/main/java/" + importedClass.replace('.', '/') + ".java");
            }
        }

        return paths;
    }

    private boolean isProjectLocal(String importedClass) {
        return SKIPPED_IMPORT_PREFIXES.stream().noneMatch(importedClass::startsWith);
    }

}
