package com.testgen.validation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Component
public class TestCompiler {

    private static final Logger log = LoggerFactory.getLogger(TestCompiler.class);

    /**
     * Compiles the given Java source file to outputDir.
     *
     * @return empty list on success; non-empty list of error messages on compile failure
     */
    public List<String> compile(Path sourceFile, Path outputDir) throws IOException {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            throw new IllegalStateException("Java compiler not available — run the application with a JDK, not a JRE");
        }

        Files.createDirectories(outputDir);

        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();

        try (StandardJavaFileManager fileManager =
                     compiler.getStandardFileManager(diagnostics, Locale.getDefault(), null)) {

            Iterable<? extends JavaFileObject> compilationUnits =
                    fileManager.getJavaFileObjects(sourceFile.toFile());

            // Explicit -classpath is required so JUnit 5 and other test imports resolve.
            // Without this, generated tests fail with "package org.junit.jupiter.api does not exist".
            List<String> options = List.of(
                    "-classpath", System.getProperty("java.class.path"),
                    "-d", outputDir.toString()
            );

            JavaCompiler.CompilationTask task =
                    compiler.getTask(null, fileManager, diagnostics, options, null, compilationUnits);
            boolean success = task.call();

            if (success) {
                log.debug("Compilation succeeded: {}", sourceFile.getFileName());
                return List.of();
            }

            List<String> errors = new ArrayList<>();
            for (Diagnostic<? extends JavaFileObject> d : diagnostics.getDiagnostics()) {
                if (d.getKind() == Diagnostic.Kind.ERROR) {
                    String sourceName = d.getSource() != null ? d.getSource().getName() : "unknown";
                    errors.add(String.format("%s (line %d): %s",
                            sourceName, d.getLineNumber(), d.getMessage(Locale.getDefault())));
                }
            }
            log.debug("Compilation failed with {} error(s) in: {}", errors.size(), sourceFile.getFileName());
            return errors;
        }
    }
}
