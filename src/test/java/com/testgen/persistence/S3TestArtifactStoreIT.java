package com.testgen.persistence;

import com.testgen.config.LocalStackAwsTestConfig;
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
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;

import java.net.URI;

import static com.testgen.util.Constants.S3_KEY_PREFIX;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
@Tag("integration")
@ActiveProfiles("test")
@Import(LocalStackAwsTestConfig.class)
class S3TestArtifactStoreIT {

    private static final String BUCKET = "test-generation-artifacts-dev";

    @Container
    static final GenericContainer<?> LOCALSTACK = new GenericContainer<>("localstack/localstack:4")
            .withEnv("SERVICES", "s3")
            .withExposedPorts(4566)
            .waitingFor(Wait.forHttp("/_localstack/health").forPort(4566));

    @DynamicPropertySource
    static void s3Properties(DynamicPropertyRegistry registry) {
        registry.add("aws.s3.endpoint", () -> "http://localhost:" + LOCALSTACK.getMappedPort(4566));
        registry.add("aws.s3.bucket-name", () -> BUCKET);
    }

    @Autowired
    S3TestArtifactStore store;

    @BeforeAll
    static void createBucket() {
        // Container is started by Testcontainers BeforeAllCallback before @BeforeAll runs.
        // A temp client is constructed directly so @Autowired isn't needed on a static method.
        try (S3Client tempClient = S3Client.builder()
                .region(Region.of("us-east-1"))
                .endpointOverride(URI.create("http://localhost:" + LOCALSTACK.getMappedPort(4566)))
                .forcePathStyle(true)
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create("test", "test"))) // these are not real credentials
                .build()) {
            tempClient.createBucket(CreateBucketRequest.builder().bucket(BUCKET).build());
        }
    }

    @Test
    void uploadsAndDownloadsTestFile() {
        String key = S3_KEY_PREFIX + "repo-s3it/run-001/FooTest.java";
        String content = "class FooTest {}";

        store.upload(content, key);
        String downloaded = store.download(key);

        assertThat(downloaded).isEqualTo(content);
    }

    @Test
    void uploadReturnsCorrectS3Uri() {
        String key = S3_KEY_PREFIX + "repo-s3it/run-002/BarTest.java";

        String uri = store.upload("class BarTest {}", key);

        assertThat(uri).isEqualTo("s3://" + BUCKET + "/" + key);
    }

    @Test
    void uploadOverwritesExistingKey() {
        String key = S3_KEY_PREFIX + "repo-s3it/run-003/BazTest.java";

        store.upload("class BazTest { /* v1 */ }", key);
        store.upload("class BazTest { /* v2 */ }", key);
        String downloaded = store.download(key);

        assertThat(downloaded).isEqualTo("class BazTest { /* v2 */ }");
    }
}
