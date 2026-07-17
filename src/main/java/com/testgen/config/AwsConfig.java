package com.testgen.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.services.s3.S3Client;

import java.net.URI;

@Configuration
@EnableConfigurationProperties(AwsProperties.class)
public class AwsConfig {

    @Bean
    DynamoDbClient dynamoDbClient(AwsProperties props) {
        var builder = DynamoDbClient.builder().region(Region.of(props.region()));
        if (props.dynamodb() != null && props.dynamodb().endpoint() != null && !props.dynamodb().endpoint().isBlank()) {
            builder.endpointOverride(URI.create(props.dynamodb().endpoint()));
        }
        return builder.build();
    }

    @Bean
    DynamoDbEnhancedClient dynamoDbEnhancedClient(DynamoDbClient dynamoDbClient) {
        return DynamoDbEnhancedClient.builder()
                .dynamoDbClient(dynamoDbClient)
                .build();
    }

    @Bean
    S3Client s3Client(AwsProperties props) {
        var builder = S3Client.builder().region(Region.of(props.region()));
        if (props.s3() != null && props.s3().endpoint() != null && !props.s3().endpoint().isBlank()) {
            builder.endpointOverride(URI.create(props.s3().endpoint()))
                   .forcePathStyle(true);
        }
        return builder.build();
    }
}
