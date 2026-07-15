package com.testgen.generation;

import com.testgen.model.GeneratedTest;
import com.testgen.model.GenerationContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;

public class TestGenerationService {

    private static final Logger log = LoggerFactory.getLogger(TestGenerationService.class);

    // Requires the class name to be followed by {, <, extends, or implements — avoids
    // false matches on prose like "no class declaration here".
    private static final Pattern CLASS_NAME_PATTERN =
            Pattern.compile("class\\s+(\\w+)(?=\\s*(?:\\{|<|extends|implements))");
    private static final Pattern PACKAGE_PATTERN = Pattern.compile("package\\s+([\\w.]+)\\s*;");
    private static final Path OUTPUT_DIR = Path.of("tmp", "generated-tests");

    private final LlmProvider llmProvider;
    private final TestGenerationPromptBuilder promptBuilder;

    public TestGenerationService(LlmProvider llmProvider, TestGenerationPromptBuilder promptBuilder) {
        this.llmProvider = llmProvider;
        this.promptBuilder = promptBuilder;
    }

    public GeneratedTest generate(GenerationContext context) {
        String systemPrompt = promptBuilder.buildSystemPrompt(context.conventions());
        String userPrompt = promptBuilder.buildUserPrompt(context);

        String rawResponse = llmProvider.generate(systemPrompt, userPrompt);
        String testCode = rawResponse.replaceAll("```java|```", "").trim();

        String className = extractClassName(testCode);
        String packageName = extractPackageName(testCode);

        Path savedPath = writeTestFile(className, testCode);
        log.info("Generated test class {} written to {}", className, savedPath);

        return new GeneratedTest(className, packageName, testCode, savedPath, Instant.now());
    }

    private String extractClassName(String testCode) {
        Matcher matcher = CLASS_NAME_PATTERN.matcher(testCode);
        if (!matcher.find()) {
            throw new IllegalStateException("LLM response contains no Java class declaration");
        }
        return matcher.group(1);
    }

    private String extractPackageName(String testCode) {
        Matcher matcher = PACKAGE_PATTERN.matcher(testCode);
        return matcher.find() ? matcher.group(1) : "com.example";
    }

    private Path writeTestFile(String className, String testCode) {
        try {
            Files.createDirectories(OUTPUT_DIR);
            Path filePath = OUTPUT_DIR.resolve(className + ".java");
            Files.writeString(filePath, testCode, CREATE, TRUNCATE_EXISTING);
            return filePath;
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to write generated test file for class " + className, e);
        }
    }
}
