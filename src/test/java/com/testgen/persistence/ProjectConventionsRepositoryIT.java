package com.testgen.persistence;

import com.testgen.model.ProjectConventions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeDefinition;
import software.amazon.awssdk.services.dynamodb.model.BillingMode;
import software.amazon.awssdk.services.dynamodb.model.CreateTableRequest;
import software.amazon.awssdk.services.dynamodb.model.KeySchemaElement;
import software.amazon.awssdk.services.dynamodb.model.KeyType;
import software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static com.testgen.util.Constants.PROJECT_CONVENTIONS;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
@Tag("integration")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ProjectConventionsRepositoryIT {

    @Container
    static final GenericContainer<?> DDB = new GenericContainer<>("amazon/dynamodb-local:2.5.2")
            .withCommand("-inMemory -sharedDb")
            .withExposedPorts(8000);

    @DynamicPropertySource
    static void dynamoDbProperties(DynamicPropertyRegistry registry) {
        registry.add("aws.dynamodb.endpoint", () -> "http://localhost:" + DDB.getMappedPort(8000));
    }

    @Autowired
    DynamoDbClient dynamoDbClient;

    @Autowired
    ProjectConventionsRepository repository;

    @BeforeAll
    void createTable() {
        dynamoDbClient.createTable(CreateTableRequest.builder()
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
