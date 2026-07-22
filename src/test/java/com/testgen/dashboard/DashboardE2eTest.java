package com.testgen.dashboard;

import com.microsoft.playwright.APIRequest;
import com.microsoft.playwright.APIRequestContext;
import com.microsoft.playwright.APIResponse;
import com.microsoft.playwright.Playwright;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
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

import static com.testgen.util.Constants.TEST_RUNS;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@Tag("e2e")
@ActiveProfiles("test")
// Intentionally not following the *.IT.java naming convention.
class DashboardE2eTest {

    @Container
    static final GenericContainer<?> LOCALSTACK = new GenericContainer<>("localstack/localstack:4")
            .withEnv("SERVICES", "dynamodb")
            .withExposedPorts(4566)
            .waitingFor(Wait.forHttp("/_localstack/health").forPort(4566));

    @DynamicPropertySource
    static void dynamoDbProperties(DynamicPropertyRegistry registry) {
        registry.add("aws.dynamodb.endpoint", () -> "http://localhost:" + LOCALSTACK.getMappedPort(4566));
    }

    @LocalServerPort
    private int port;

    private static Playwright playwright;
    private APIRequestContext requestContext;

    @BeforeAll
    static void createTableAndPlaywright() {
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

        playwright = Playwright.create();
    }

    @AfterAll
    static void closePlaywright() {
        playwright.close();
    }

    @Test
    void dashboardEndpointReturns200WithTotalTestRunsField() {
        requestContext = playwright.request().newContext(
                new APIRequest.NewContextOptions().setBaseURL("http://localhost:" + port));

        APIResponse response = requestContext.get("/api/v1/dashboard?repositoryId=test");

        assertThat(response.status()).isEqualTo(200);
        assertThat(response.text()).contains("totalTestRuns");

        requestContext.dispose();
    }
}
