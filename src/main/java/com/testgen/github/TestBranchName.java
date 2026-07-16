package com.testgen.github;

import java.util.UUID;

public record TestBranchName(String value) {

    static TestBranchName from(String sourceBranch) {
        return new TestBranchName("testgen/" + sourceBranch + "-" + UUID.randomUUID().toString().substring(0, 8));
    }
}
