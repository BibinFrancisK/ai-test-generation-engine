package com.testgen.healing;

import com.testgen.model.TestFailure;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class JUnitXmlReportParserTest {

    private static String fixtureReport;
    private final JUnitXmlReportParser parser = new JUnitXmlReportParser();

    @BeforeAll
    static void loadFixture() throws IOException {
        InputStream stream = JUnitXmlReportParserTest.class.getResourceAsStream("/fixtures/surefire-report.xml");
        assertNotNull(stream, "surefire-report.xml fixture must exist on the classpath");
        fixtureReport = new String(stream.readAllBytes());
    }

    @Test
    @DisplayName("ignores passing test cases and extracts only the failing ones")
    void extractsOnlyFailingTestCases() {
        List<TestFailure> failures = parser.parse(fixtureReport);

        assertThat(failures).hasSize(2);
        assertThat(failures).noneMatch(f -> f.testMethodName().equals("getStatusReturnsOk"));
    }

    @Test
    void extractsClassAndMethodNamesFromFailures() {
        List<TestFailure> failures = parser.parse(fixtureReport);

        assertThat(failures)
                .extracting(TestFailure::testClassName, TestFailure::testMethodName)
                .containsExactlyInAnyOrder(
                        tuple("com.example.service.SampleServiceTest", "processOrderReturnsFormattedString"),
                        tuple("com.example.service.SampleServiceTest", "saveOrderDelegatesToRepository"));
    }

    @Test
    void extractsErrorMessageAndStackTraceFromFailures() {
        List<TestFailure> failures = parser.parse(fixtureReport);

        TestFailure processOrderFailure = failures.stream()
                .filter(f -> f.testMethodName().equals("processOrderReturnsFormattedString"))
                .findFirst()
                .orElseThrow();

        assertThat(processOrderFailure.errorMessage()).contains("expected: <ord-001:5> but was: <ord-001-5>");
        assertThat(processOrderFailure.stackTrace()).contains("SampleServiceTest.processOrderReturnsFormattedString");
    }

    @Test
    void returnsEmptyListForBlankInput() {
        assertThat(parser.parse("")).isEmpty();
        assertThat(parser.parse("   ")).isEmpty();
    }

    @Test
    void returnsEmptyListForNullInput() {
        assertThat(parser.parse(null)).isEmpty();
    }

    @Test
    @DisplayName("returns no failures instead of throwing when the report is malformed")
    void returnsEmptyListForMalformedXml() {
        assertThat(parser.parse("<testsuite><testcase not-closed")).isEmpty();
    }
}
