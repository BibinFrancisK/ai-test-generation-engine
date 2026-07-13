package com.testgen.persistence;

import com.testgen.model.ProjectConventions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import java.time.Instant;
import java.util.Optional;

import static com.testgen.util.Constants.PROJECT_CONVENTIONS;

@Repository
public class ProjectConventionsRepository {

    private static final Logger log = LoggerFactory.getLogger(ProjectConventionsRepository.class);

    private static final TableSchema<ProjectConventionsEntity> TABLE_SCHEMA = TableSchema.fromBean(ProjectConventionsEntity.class);
    private final DynamoDbTable<ProjectConventionsEntity> table;

    public ProjectConventionsRepository(DynamoDbEnhancedClient enhancedClient) {
        this.table = enhancedClient.table(PROJECT_CONVENTIONS, TABLE_SCHEMA);
    }

    public void save(ProjectConventions conventions) {
        table.putItem(toEntity(conventions));
        log.debug("Saved ProjectConventions for repository {}", conventions.repositoryId());
    }

    public Optional<ProjectConventions> findByRepositoryId(String repositoryId) {
        ProjectConventionsEntity entity = table.getItem(Key.builder()
                .partitionValue(repositoryId)
                .build());
        return Optional.ofNullable(entity).map(ProjectConventionsRepository::toProjectConventions);
    }

    private static ProjectConventionsEntity toEntity(ProjectConventions c) {
        ProjectConventionsEntity e = new ProjectConventionsEntity();
        e.setRepositoryId(c.repositoryId());
        e.setSchemaVersion(c.schemaVersion());
        e.setTestingFramework(c.testingFramework());
        e.setMockLibrary(c.mockLibrary());
        e.setBaseTestClassName(c.baseTestClassName().orElse(null));
        e.setTestPackagePattern(c.testPackagePattern());
        e.setAnalyzedAt(c.analyzedAt().toString());
        return e;
    }

    private static ProjectConventions toProjectConventions(ProjectConventionsEntity e) {
        return new ProjectConventions(
                e.getRepositoryId(),
                e.getSchemaVersion(),
                e.getTestingFramework(),
                e.getMockLibrary(),
                Optional.ofNullable(e.getBaseTestClassName()),
                e.getTestPackagePattern(),
                Instant.parse(e.getAnalyzedAt())
        );
    }
}
