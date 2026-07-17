package com.testgen.persistence;

import com.testgen.config.LocalStackAwsTestConfig;
import com.testgen.model.TestRun;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeDefinition;
import software.amazon.awssdk.services.dynamodb.model.BillingMode;
import software.amazon.awssdk.services.dynamodb.model.CreateTableRequest;
import software.amazon.awssdk.services.dynamodb.model.KeySchemaElement;
import software.amazon.awssdk.services.dynamodb.model.KeyType;
import software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType;

import java.net.URI;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static com.testgen.util.Constants.TEST_RUNS;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
@Tag("integration")
@ActiveProfiles("test")
@Import(LocalStackAwsTestConfig.class)
class DynamoDbTestRepositoryIT {

    @Container
    static final GenericContainer<?> LOCALSTACK = new GenericContainer<>("localstack/localstack:4")
            .withEnv("SERVICES", "dynamodb")
            .withExposedPorts(4566)
            .waitingFor(Wait.forHttp("/_localstack/health").forPort(4566));

    @DynamicPropertySource
    static void dynamoDbProperties(DynamicPropertyRegistry registry) {
        registry.add("aws.dynamodb.endpoint", () -> "http://localhost:" + LOCALSTACK.getMappedPort(4566));
    }

    @Autowired
    DynamoDbTestRepository repository;

    @BeforeAll
    static void createTable() {
        try (DynamoDbClient client = DynamoDbClient.builder()
                .region(Region.of("us-east-1"))
                .endpointOverride(URI.create("http://localhost:" + LOCALSTACK.getMappedPort(4566)))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create("test", "test"))) // these are not real credentials
                .build()) {
            client.createTable(CreateTableRequest.builder()
                    .tableName(TEST_RUNS)
                    .keySchema(
                            KeySchemaElement.builder().attributeName("testRunId").keyType(KeyType.HASH).build(),
                            KeySchemaElement.builder().attributeName("repositoryId").keyType(KeyType.RANGE).build()
                    )
                    .attributeDefinitions(
                            AttributeDefinition.builder().attributeName("testRunId").attributeType(ScalarAttributeType.S).build(),
                            AttributeDefinition.builder().attributeName("repositoryId").attributeType(ScalarAttributeType.S).build()
                    )
                    .billingMode(BillingMode.PAY_PER_REQUEST)
                    .build());
        }
    }

    @Test
    void savesAndRetrievesTestRun() {
        TestRun testRun = new TestRun(
                "run-001",
                "repo-abc",
                "pr-101",
                "class FooTest {}",
                "SUCCESS",
                Instant.now().truncatedTo(ChronoUnit.SECONDS).toString(),
                "s3://bucket/FooTest.java",
                "https://github.com/owner/repo/pull/1",
                "v1"
        );

        repository.save(testRun);
        List<TestRun> results = repository.findByRepositoryAndPr("repo-abc", "pr-101");

        assertThat(results).hasSize(1);
        TestRun retrieved = results.getFirst();
        assertThat(retrieved.testRunId()).isEqualTo("run-001");
        assertThat(retrieved.repositoryId()).isEqualTo("repo-abc");
        assertThat(retrieved.pullRequestId()).isEqualTo("pr-101");
        assertThat(retrieved.generatedTestCode()).isEqualTo("class FooTest {}");
        assertThat(retrieved.validationStatus()).isEqualTo("SUCCESS");
        assertThat(retrieved.s3ArtifactKey()).isEqualTo("s3://bucket/FooTest.java");
        assertThat(retrieved.schemaVersion()).isEqualTo("v1");
    }

    @Test
    void handlesNullS3ArtifactKey() {
        TestRun testRun = new TestRun(
                "run-002",
                "repo-abc",
                "pr-102",
                "class BarTest {}",
                "FAILED",
                Instant.now().truncatedTo(ChronoUnit.SECONDS).toString(),
                null,
                null,
                "v1"
        );

        repository.save(testRun);
        List<TestRun> results = repository.findByRepositoryAndPr("repo-abc", "pr-102");

        assertThat(results).hasSize(1);
        assertThat(results.getFirst().s3ArtifactKey()).isNull();
    }

    @Test
    void returnsEmptyListWhenNothingMatches() {
        List<TestRun> results = repository.findByRepositoryAndPr("repo-does-not-exist", "pr-999");

        assertThat(results).isEmpty();
    }
}
