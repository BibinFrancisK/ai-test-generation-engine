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
}
