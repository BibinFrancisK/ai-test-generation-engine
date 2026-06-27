# ADR-003 — Test Storage: DynamoDB (metadata) + S3 (artifacts)

## Context

The service produces two kinds of output that must be persisted:
1. **Test-run metadata** — status, PR links, validation errors, timestamps — queried by `testRunId` and `repositoryId`
2. **Generated test source files** — `.java` files, 1–10 KB each, accessed by engineers who want to inspect what was generated

Constraints: AWS free tier for all portfolio-scale traffic; schemaless to accommodate evolving `TestRun` fields; no relational joins needed.

## Decision

**DynamoDB Enhanced Client** for metadata; **S3** for generated test files.

Two DynamoDB tables:
- `test-runs` — partition key `testRunId`; stores `TestRun` record including status, `s3ArtifactUrl`, PR URL, validation errors, `schemaVersion`
- `project-conventions` — partition key `repositoryId`; stores `ProjectConventions` record including detected test framework, mock library, base class, `analyzedAt`

One S3 bucket for test artifacts:
- Private ACL; `block_public_access = true` in Terraform
- Access via presigned URLs (TTL ≤ 1 hour) only
- Object key: `test-artifacts/{repositoryId}/{testRunId}/{ClassName}Test.java`

Both tables and the bucket are provisioned via Terraform and emulated locally by LocalStack.

## Consequences

- DynamoDB free tier (25 GB + 25 WCU/RCU) covers all portfolio traffic with room to spare
- S3 keeps raw file content out of DynamoDB, which enforces a 400 KB item size limit
- Schema evolution handled by `schemaVersion` field on every stored record — never remove or rename existing DynamoDB attributes
- No relational queries needed — all access patterns are key lookups, which DynamoDB handles optimally
- RDS/PostgreSQL rejected: operational overhead (VPC, subnet groups, snapshots) not justified for key-value access patterns
