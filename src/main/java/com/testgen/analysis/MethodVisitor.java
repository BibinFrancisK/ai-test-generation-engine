package com.testgen.analysis;

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.testgen.model.ChangedMethod;
import com.testgen.util.Constants;

import java.util.List;

class MethodVisitor extends VoidVisitorAdapter<List<ChangedMethod>> {

    @Override
    public void visit(MethodDeclaration method, List<ChangedMethod> collector) {
        super.visit(method, collector);

        String className = method.findAncestor(ClassOrInterfaceDeclaration.class)
                .map(ClassOrInterfaceDeclaration::getNameAsString)
                .orElse(Constants.UNKNOWN_CLASS);

        List<String> parameterTypes = method.getParameters().stream()
                .map(p -> p.getTypeAsString())
                .toList();

        List<String> annotations = method.getAnnotations().stream()
                .map(a -> a.getNameAsString())
                .toList();

        int startLine = method.getBegin().map(pos -> pos.line).orElse(0);
        int endLine = method.getEnd().map(pos -> pos.line).orElse(0);

        collector.add(new ChangedMethod(
                className,
                method.getNameAsString(),
                parameterTypes,
                method.getTypeAsString(),
                annotations,
                startLine,
                endLine
        ));
    }
}
