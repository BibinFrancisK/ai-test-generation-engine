package com.testgen.validation;

import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import org.junit.platform.launcher.listeners.TestExecutionSummary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Component
public class TestExecutor {

    private static final Logger log = LoggerFactory.getLogger(TestExecutor.class);

    /**
     * Executes the compiled JUnit 5 class at outputDir via the JUnit Platform Launcher.
     * The URLClassLoader step is mandatory — selectClass(Class<?>) needs a loaded class,
     * not a file path, and the class must be loaded from the compiler's output directory.
     *
     * @param outputDir directory containing compiled .class files
     * @param className fully-qualified class name (e.g. "com.example.service.SampleServiceTest")
     */
    public ExecutionResult execute(Path outputDir, String className) {
        try (URLClassLoader loader = new URLClassLoader(
                new URL[]{outputDir.toUri().toURL()},
                getClass().getClassLoader())) {
            Class<?> testClass = loader.loadClass(className);

            List<String> passedTests = new ArrayList<>();

            SummaryGeneratingListener summaryListener = new SummaryGeneratingListener();
            TestExecutionListener nameCapture = new TestExecutionListener() {
                @Override
                public void executionFinished(TestIdentifier id, TestExecutionResult result) {
                    if (id.isTest() && result.getStatus() == TestExecutionResult.Status.SUCCESSFUL) {
                        passedTests.add(id.getDisplayName());
                    }
                }
            };

            LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
                    .selectors(DiscoverySelectors.selectClass(testClass))
                    .build();

            Launcher launcher = LauncherFactory.create();
            launcher.execute(request, summaryListener, nameCapture);

            TestExecutionSummary summary = summaryListener.getSummary();

            List<String> failedTests = new ArrayList<>();
            List<String> errorMessages = new ArrayList<>();
            for (TestExecutionSummary.Failure failure : summary.getFailures()) {
                String displayName = failure.getTestIdentifier().getDisplayName();
                String message = failure.getException() != null
                        ? failure.getException().getMessage()
                        : "unknown error";
                String entry = displayName + ": " + message;
                failedTests.add(entry);
                errorMessages.add(entry);
            }

            log.debug("Executed {}: {} passed, {} failed",
                    className, summary.getTestsSucceededCount(), summary.getTestsFailedCount());
            return new ExecutionResult(passedTests, failedTests, errorMessages);

        } catch (Exception e) {
            log.warn("Could not load or execute class {}: {}", className, e.getMessage());
            return new ExecutionResult(List.of(), List.of(), List.of("Execution error: " + e.getMessage()));
        }
    }
}
