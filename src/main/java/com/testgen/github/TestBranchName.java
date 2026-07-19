package com.testgen.github;

import java.util.UUID;

import static com.testgen.util.Constants.TESTGEN_BRANCH_PREFIX;

public record TestBranchName(String value) {

    static TestBranchName from(String sourceBranch) {
        return new TestBranchName(TESTGEN_BRANCH_PREFIX + sourceBranch + "-" + UUID.randomUUID().toString().substring(0, 8));
    }
}
