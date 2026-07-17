package com.testgen.persistence;

import com.testgen.config.LocalStackAwsTestConfig;
import com.testgen.model.ProjectConventions;
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
import java.util.Optional;

import static com.testgen.util.Constants.PROJECT_CONVENTIONS;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
@Tag("integration")
@ActiveProfiles("test")
@Import(LocalStackAwsTestConfig.class)
class ProjectConventionsRepositoryIT {

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
    ProjectConventionsRepository repository;

    @BeforeAll
    static void createTable() {
        try (DynamoDbClient client = DynamoDbClient.builder()
                .region(Region.of("us-east-1"))
                .endpointOverride(URI.create("http://localhost:" + LOCALSTACK.getMappedPort(4566)))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create("test", "test"))) // these are not real credentials
                .build()) {
            client.createTable(CreateTableRequest.builder()
                    .tableName(PROJECT_CONVENTIONS)
                    .keySchema(
                            KeySchemaElement.builder().attributeName("repositoryId").keyType(KeyType.HASH).build()
                    )
                    .attributeDefinitions(
                            AttributeDefinition.builder().attributeName("repositoryId").attributeType(ScalarAttributeType.S).build()
                    )
                    .billingMode(BillingMode.PAY_PER_REQUEST)
                    .build());
        }
    }

    @Test
    void savesAndRetrievesConventions() {
        Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        ProjectConventions conventions = new ProjectConventions(
                "repo-xyz",
                "v1",
                "junit5",
                "mockito",
                Optional.of("AbstractBaseTest"),
                "mirrors-source",
                now
        );

        repository.save(conventions);
        Optional<ProjectConventions> result = repository.findByRepositoryId("repo-xyz");

        assertThat(result).isPresent();
        ProjectConventions retrieved = result.get();
        assertThat(retrieved.repositoryId()).isEqualTo("repo-xyz");
        assertThat(retrieved.schemaVersion()).isEqualTo("v1");
        assertThat(retrieved.testingFramework()).isEqualTo("junit5");
        assertThat(retrieved.mockLibrary()).isEqualTo("mockito");
        assertThat(retrieved.baseTestClassName()).hasValue("AbstractBaseTest");
        assertThat(retrieved.testPackagePattern()).isEqualTo("mirrors-source");
        assertThat(retrieved.analyzedAt()).isEqualTo(now);
    }

    @Test
    void preservesEmptyBaseTestClassName() {
        Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        ProjectConventions conventions = new ProjectConventions(
                "repo-no-base",
                "v1",
                "junit5",
                "mockito",
                Optional.empty(),
                "mirrors-source",
                now
        );

        repository.save(conventions);
        Optional<ProjectConventions> result = repository.findByRepositoryId("repo-no-base");

        assertThat(result).isPresent();
        assertThat(result.get().baseTestClassName()).isEmpty();
    }

    @Test
    void returnsEmptyForUnknownRepositoryId() {
        Optional<ProjectConventions> result = repository.findByRepositoryId("repo-does-not-exist");

        assertThat(result).isEmpty();
    }
}
