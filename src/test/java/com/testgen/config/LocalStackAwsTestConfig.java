package com.testgen.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.s3.S3Client;

import java.net.URI;

/**
 * Overrides {@link AwsConfig}'s DynamoDbClient/S3Client beans with LocalStack-only static
 * credentials — LocalStack never validates them, but the SDK still refuses to make a call
 * without something resolving. {@code @Primary} lets this coexist with the production beans
 * (different bean names, no bean-definition-overriding needed) so real AWS wiring in
 * {@code src/main} never has to know test credentials exist.
 */
@TestConfiguration
public class LocalStackAwsTestConfig {

    private static final StaticCredentialsProvider LOCALSTACK_CREDENTIALS =
            StaticCredentialsProvider.create(AwsBasicCredentials.create("test", "test"));

    @Bean
    @Primary
    DynamoDbClient localStackDynamoDbClient(AwsProperties props) {
        var builder = DynamoDbClient.builder().region(Region.of(props.region()));
        if (props.dynamodb() != null && props.dynamodb().endpoint() != null && !props.dynamodb().endpoint().isBlank()) {
            builder.endpointOverride(URI.create(props.dynamodb().endpoint()))
                   .credentialsProvider(LOCALSTACK_CREDENTIALS);
        }
        return builder.build();
    }

    @Bean
    @Primary
    S3Client localStackS3Client(AwsProperties props) {
        var builder = S3Client.builder().region(Region.of(props.region()));
        if (props.s3() != null && props.s3().endpoint() != null && !props.s3().endpoint().isBlank()) {
            builder.endpointOverride(URI.create(props.s3().endpoint()))
                   .credentialsProvider(LOCALSTACK_CREDENTIALS)
                   .forcePathStyle(true);
        }
        return builder.build();
    }
}
