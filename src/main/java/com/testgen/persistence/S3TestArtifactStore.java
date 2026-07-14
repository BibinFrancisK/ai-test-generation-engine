package com.testgen.persistence;

import com.testgen.config.AwsProperties;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.nio.charset.StandardCharsets;

import static com.testgen.util.Constants.S3_KEY_PREFIX;

@Repository
public class S3TestArtifactStore {

    private final S3Client s3Client;
    private final String bucketName;

    public S3TestArtifactStore(S3Client s3Client, AwsProperties props) {
        this.s3Client = s3Client;
        this.bucketName = props.s3().bucketName();
    }

    /**
     * Uploads generated test code to S3 and returns the S3 URI.
     * Key format: test-artifacts/{repositoryId}/{testRunId}/{ClassName}Test.java
     * Called by TestGenerationOrchestrator after validation, before DynamoDbTestRepository.save().
     */
    public String upload(String testCode, String key) {
        s3Client.putObject(
                PutObjectRequest.builder()
                        .bucket(bucketName)
                        .key(key)
                        .contentType("text/x-java-source")
                        .build(),
                RequestBody.fromBytes(testCode.getBytes(StandardCharsets.UTF_8))
        );
        return "s3://" + bucketName + "/" + key;
    }

    public String download(String key) {
        return s3Client.getObjectAsBytes(
                GetObjectRequest.builder()
                        .bucket(bucketName)
                        .key(key)
                        .build()
        ).asUtf8String();
    }

    public static String buildKey(String repositoryId, String testRunId, String className) {
        return S3_KEY_PREFIX + repositoryId + "/" + testRunId + "/" + className + "Test.java";
    }
}
