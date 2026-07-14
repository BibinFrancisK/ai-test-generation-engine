package com.testgen.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "aws")
public record AwsProperties(String region, DynamoDb dynamodb, S3 s3) {

    public record DynamoDb(String endpoint) {
    }

    public record S3(String endpoint, String bucketName) {
    }
}
