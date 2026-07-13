package com.testgen.persistence;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

@DynamoDbBean
public class ProjectConventionsEntity {
    private String repositoryId;
    private String schemaVersion;
    private String testingFramework;
    private String mockLibrary;
    private String baseTestClassName; // null when Optional.empty()
    private String testPackagePattern;
    private String analyzedAt;        // ISO-8601 string; Instant does not map natively to DynamoDB

    @DynamoDbPartitionKey
    public String getRepositoryId() { return repositoryId; }
    public void setRepositoryId(String repositoryId) { this.repositoryId = repositoryId; }

    public String getSchemaVersion() { return schemaVersion; }
    public void setSchemaVersion(String schemaVersion) { this.schemaVersion = schemaVersion; }

    public String getTestingFramework() { return testingFramework; }
    public void setTestingFramework(String testingFramework) { this.testingFramework = testingFramework; }

    public String getMockLibrary() { return mockLibrary; }
    public void setMockLibrary(String mockLibrary) { this.mockLibrary = mockLibrary; }

    public String getBaseTestClassName() { return baseTestClassName; }
    public void setBaseTestClassName(String baseTestClassName) { this.baseTestClassName = baseTestClassName; }

    public String getTestPackagePattern() { return testPackagePattern; }
    public void setTestPackagePattern(String testPackagePattern) { this.testPackagePattern = testPackagePattern; }

    public String getAnalyzedAt() { return analyzedAt; }
    public void setAnalyzedAt(String analyzedAt) { this.analyzedAt = analyzedAt; }
}
