package com.testgen.util;

public final class Constants {

    private Constants() {}

    // SourceAnalyzer
    public static final String UNKNOWN_CLASS = "Unknown";

    // ProjectConventionsAnalyzer — import markers
    public static final String IMPORT_JUNIT5 = "import org.junit.jupiter";
    public static final String IMPORT_JUNIT4_TEST = "import org.junit.Test";
    public static final String IMPORT_MOCKITO = "import org.mockito";
    public static final String IMPORT_EASYMOCK = "import org.easymock";
    public static final String MOCKITO_EXTENSION_CLASS = "MockitoExtension.class";

    // ProjectConventionsAnalyzer — annotation name used in base-class detection
    public static final String ANNOTATION_TEST = "Test";

    // ProjectConventionsAnalyzer — framework and mock library names
    public static final String FRAMEWORK_JUNIT5 = "junit5";
    public static final String FRAMEWORK_JUNIT4 = "junit4";
    public static final String MOCK_LIB_MOCKITO = "mockito";
    public static final String MOCK_LIB_EASYMOCK = "easymock";

    // ProjectConventions record defaults
    public static final String SCHEMA_VERSION_V1 = "v1";
    public static final String TEST_PACKAGE_PATTERN_MIRRORS_SOURCE = "mirrors-source";

    // DiffParser — unified diff markers and hunk header regex
    public static final String DIFF_HUNK_HEADER_REGEX = "^@@ -\\d+(?:,\\d+)? \\+(\\d+)(?:,(\\d+))? @@";
    public static final String DIFF_NEW_FILE_MARKER = "+++ /dev/null";
    public static final String DIFF_OLD_FILE_MARKER = "--- /dev/null";

    public static final int HARD_MAX_TOKENS = 1500;

    // LLM provider identifiers — used in application.yml and AppConfig switch
    public static final String ANTHROPIC = "anthropic";
    public static final String OPENAI = "openai";
    public static final String NOOP = "noop";

    // TestGenerationPromptBuilder — prompt templates
    public static final String SYSTEM_PROMPT = """
            You are an expert Java test engineer. Generate JUnit 5 unit tests using Mockito.
            Return only valid Java source code — no markdown code fences, no explanations.""";

    public static final String USER_PROMPT_TEMPLATE = """
            Generate JUnit 5 tests for the following Java class and its changed methods.

            Class: %s

            Changed methods:
            %s
            """;

    public static final String PROMPT_METHOD_SIGNATURE_FORMAT = "  %s%s %s(%s)";
    public static final String PROJECT_CONVENTIONS = "project-conventions";
    public static final String TEST_RUNS = "test-runs";

    // S3TestArtifactStore — object key prefix
    public static final String S3_KEY_PREFIX = "test-artifacts/";
}
