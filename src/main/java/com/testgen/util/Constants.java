package com.testgen.util;

import java.time.Duration;
import java.util.List;
import java.util.regex.Pattern;

public final class Constants {

    private Constants() {
    }

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
    public static final String SYSTEM_PROMPT_TEMPLATE = """
            You are an expert Java test engineer. Use %s and %s.
            Return only valid Java source code — no markdown code fences, no explanations.""";

    // HealingPromptBuilder — prompt template and section headers
    public static final String HEALING_SYSTEM_PROMPT = """
            You are an expert Java test engineer. A test has broken due to a code change.
            Return only the corrected test class, no markdown, no explanations.""";

    public static final String EXISTING_TEST_FILE_HEADER = "## Existing test file — follow this style exactly";
    public static final String FULL_SOURCE_HEADER = "## Full source of the changed class";
    public static final String DEPENDENCY_SOURCES_HEADER = "## Dependency sources";
    public static final String CHANGED_METHODS_HEADER = "## Changed methods";

    public static final String PROMPT_METHOD_SIGNATURE_FORMAT = "  %s%s %s(%s)";
    public static final int PROMPT_TOKEN_BUDGET = 2000;
    public static final String PROJECT_CONVENTIONS = "project-conventions";
    public static final String TEST_RUNS = "test-runs";

    // S3TestArtifactStore — object key prefix
    public static final String S3_KEY_PREFIX = "test-artifacts/";

    public static final String HMAC_ALGORITHM = "HmacSHA256";
    public static final String SIGNATURE_PREFIX = "sha256=";
    public static final String TEST_SOURCE_ROOT = "src/test/java";
    public static final List<String> SKIPPED_IMPORT_PREFIXES = List.of("java.", "org.springframework.", "org.junit.");
    public static final Pattern IMPORT_PATTERN = Pattern.compile("import\\s+([\\w.]+)\\s*;");
    public static final int MAX_DEPENDENCY_FILES = 3;
    public static final int MAX_CONVENTION_TEST_FILES = 5;
    public static final Duration CONVENTIONS_TTL = Duration.ofDays(7);

    // TestGenerationOrchestrator / TestGenerationController — TestRun.validationStatus values
    public static final String STATUS_SUCCESS = "SUCCESS";
    public static final String STATUS_COMPILE_FAILED = "COMPILE_FAILED";
    public static final String STATUS_EXECUTION_FAILED = "EXECUTION_FAILED";
    public static final String STATUS_FAILED = "FAILED";

    // GitHubAppAuthenticator — JWT signing and installation token cache
    public static final String JWT_SIGNATURE_ALGORITHM = "SHA256withRSA";
    public static final String JWT_HEADER_JSON = "{\"alg\":\"RS256\",\"typ\":\"JWT\"}";
    public static final Duration JWT_IAT_BACKDATE = Duration.ofSeconds(60);
    public static final Duration JWT_TTL = Duration.ofMinutes(9);
    public static final Duration INSTALLATION_TOKEN_REFRESH_MARGIN = Duration.ofMinutes(1);

    // GitHubPrCreator — retry backoff for 5xx responses
    public static final Duration GITHUB_RETRY_BACKOFF = Duration.ofMillis(500);

    public static final String TESTGEN_BRANCH_PREFIX = "testgen/";

    public static final List<String> FAILURE_TAGS = List.of("failure", "error");

    public static final String HEALING_FAILING_TEST_HEADER = "## Failing test";
    public static final String HEALING_CURRENT_SOURCE_HEADER = "## Current source of the class under test";
    public static final String HEALING_ERROR_MESSAGE_HEADER = "## Error message";
    public static final String HEALING_STACK_TRACE_HEADER = "## Stack trace";
    public static final String HEALING_PREVIOUS_ERRORS_HEADER = "## Previous attempt's validation errors";
    public static final int HEALING_PROMPT_TOKEN_BUDGET = 1000;

    // HealingOrchestrator — HealedTestSummary.status values
    public static final String HEALING_STATUS_HEALED = "HEALED";
    public static final String HEALING_STATUS_FAILED = "FAILED";
    public static final String HEALING_STATUS_SKIPPED = "SKIPPED";

}
