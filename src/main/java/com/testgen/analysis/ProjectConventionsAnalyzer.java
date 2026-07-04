package com.testgen.analysis;

import com.github.javaparser.ParseProblemException;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.testgen.model.ProjectConventions;
import com.testgen.util.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class ProjectConventionsAnalyzer {

    private static final Logger log = LoggerFactory.getLogger(ProjectConventionsAnalyzer.class);

    public ProjectConventions analyze(String repositoryId, List<String> testSourceContents) {
        if (testSourceContents.isEmpty()) {
            return defaults(repositoryId);
        }

        boolean hasJUnit5 = false;
        boolean hasJUnit4 = false;
        boolean hasMockito = false;
        boolean hasEasyMock = false;
        Map<String, Integer> baseClassCounts = new HashMap<>();

        for (String source : testSourceContents) {
            if (source.contains(Constants.IMPORT_JUNIT5)) {
                hasJUnit5 = true;
            }
            if (source.contains(Constants.IMPORT_JUNIT4_TEST)) {
                hasJUnit4 = true;
            }
            if (source.contains(Constants.IMPORT_MOCKITO) || source.contains(Constants.MOCKITO_EXTENSION_CLASS)) {
                hasMockito = true;
            }
            if (source.contains(Constants.IMPORT_EASYMOCK)) {
                hasEasyMock = true;
            }

            try {
                StaticJavaParser.parse(source)
                        .findAll(ClassOrInterfaceDeclaration.class)
                        .forEach(clazz -> detectBaseClass(clazz, baseClassCounts));
            } catch (ParseProblemException e) {
                log.warn("Skipping unparseable test source during convention detection: {}", e.getMessage());
            }
        }

        // JUnit 5 wins ties
        String framework = hasJUnit5 ? Constants.FRAMEWORK_JUNIT5 : (hasJUnit4 ? Constants.FRAMEWORK_JUNIT4 : Constants.FRAMEWORK_JUNIT5);
        String mockLibrary = hasMockito ? Constants.MOCK_LIB_MOCKITO : (hasEasyMock ? Constants.MOCK_LIB_EASYMOCK : Constants.MOCK_LIB_MOCKITO);

        String mostCommonBase = baseClassCounts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);

        return new ProjectConventions(
                repositoryId,
                Constants.SCHEMA_VERSION_V1,
                framework,
                mockLibrary,
                Optional.ofNullable(mostCommonBase),
                Constants.TEST_PACKAGE_PATTERN_MIRRORS_SOURCE,
                Instant.now()
        );
    }

    private void detectBaseClass(ClassOrInterfaceDeclaration clazz, Map<String, Integer> counts) {
        boolean hasTestMethod = clazz.getMethods().stream()
                .anyMatch(m -> m.getAnnotations().stream()
                        .anyMatch(a -> a.getNameAsString().equals(Constants.ANNOTATION_TEST)));

        if (hasTestMethod && !clazz.getExtendedTypes().isEmpty()) {
            String baseClass = clazz.getExtendedTypes().get(0).getNameAsString();
            counts.merge(baseClass, 1, Integer::sum);
        }
    }

    private ProjectConventions defaults(String repositoryId) {
        return new ProjectConventions(
                repositoryId,
                Constants.SCHEMA_VERSION_V1,
                Constants.FRAMEWORK_JUNIT5,
                Constants.MOCK_LIB_MOCKITO,
                Optional.empty(),
                Constants.TEST_PACKAGE_PATTERN_MIRRORS_SOURCE,
                Instant.now()
        );
    }
}
