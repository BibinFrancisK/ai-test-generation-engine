package com.testgen.persistence;

import com.testgen.model.TestRun;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.ScanEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.List;
import java.util.Map;

import static com.testgen.util.Constants.TEST_RUNS;

@Repository
public class DynamoDbTestRepository {

    private static final Logger log = LoggerFactory.getLogger(DynamoDbTestRepository.class);

    private static final TableSchema<TestRunEntity> TABLE_SCHEMA = TableSchema.fromBean(TestRunEntity.class);
    private final DynamoDbTable<TestRunEntity> table;

    public DynamoDbTestRepository(DynamoDbEnhancedClient enhancedClient) {
        this.table = enhancedClient.table(TEST_RUNS, TABLE_SCHEMA);
    }

    public void save(TestRun testRun) {
        table.putItem(toEntity(testRun));
        log.debug("Saved TestRun {}", testRun.testRunId());
    }

    public List<TestRun> findByRepositoryAndPr(String repositoryId, String pullRequestId) {
        ScanEnhancedRequest request = ScanEnhancedRequest.builder()
                .filterExpression(Expression.builder()
                        .expression("#rid = :rid AND #prid = :prid")
                        .expressionNames(Map.of("#rid", "repositoryId", "#prid", "pullRequestId"))
                        .expressionValues(Map.of(
                                ":rid", AttributeValue.builder().s(repositoryId).build(),
                                ":prid", AttributeValue.builder().s(pullRequestId).build()))
                        .build())
                .build();
        return table.scan(request).items().stream()
                .map(DynamoDbTestRepository::toTestRun)
                .toList();
    }

    private static TestRunEntity toEntity(TestRun r) {
        TestRunEntity e = new TestRunEntity();
        e.setTestRunId(r.testRunId());
        e.setRepositoryId(r.repositoryId());
        e.setPullRequestId(r.pullRequestId());
        e.setGeneratedTestCode(r.generatedTestCode());
        e.setValidationStatus(r.validationStatus());
        e.setCreatedAt(r.createdAt());
        e.setS3ArtifactKey(r.s3ArtifactKey());
        e.setTestPrUrl(r.testPrUrl());
        e.setSchemaVersion(r.schemaVersion());
        return e;
    }

    private static TestRun toTestRun(TestRunEntity e) {
        return new TestRun(
                e.getTestRunId(),
                e.getRepositoryId(),
                e.getPullRequestId(),
                e.getGeneratedTestCode(),
                e.getValidationStatus(),
                e.getCreatedAt(),
                e.getS3ArtifactKey(),
                e.getTestPrUrl(),
                e.getSchemaVersion()
        );
    }
}
