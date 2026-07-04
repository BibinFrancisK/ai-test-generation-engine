package com.testgen.analysis;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.testgen.model.ChangedMethod;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class SourceAnalyzer {

    public List<ChangedMethod> analyze(String sourceCode) {
        CompilationUnit unit = StaticJavaParser.parse(sourceCode);
        List<ChangedMethod> methods = new ArrayList<>();
        unit.accept(new MethodVisitor(), methods);
        return List.copyOf(methods);
    }
}
