# ADR-004 — Local AWS Emulation: LocalStack

## Context

The service makes AWS SDK calls to DynamoDB, S3, and SSM Parameter Store. During development and CI, these calls must work without a live AWS account to:
- Eliminate AWS cost during development (DynamoDB writes, S3 uploads)
- Allow `mvn verify` to run in CI without AWS credentials
- Enable full Terraform plan/apply cycles against a local environment

Options evaluated: LocalStack, Moto (Python), DynamoDB Local (standalone JAR), real AWS with a dev account.

## Decision

**LocalStack** via Docker Compose and Testcontainers.

- `docker-compose.yml` starts LocalStack as the default service (no `--profile` needed for local dev)
- Integration tests use `LocalStackContainer` from the Testcontainers library — container declared `static` for reuse across tests in a class
- Ports wired via `@DynamicPropertySource` — never hardcoded
- LocalStack image pinned to an exact version in `docker-compose.yml` and Testcontainers config — no `latest` tag

Terraform also targets LocalStack for local infra validation (`AWS_ENDPOINT_URL` override).

## Consequences

- Zero AWS cost during development and CI
- Full Terraform parity: the same `.tf` files that provision LocalStack locally provision real AWS in production
- Testcontainers startup adds ~10–15 s to the first integration test run per class; `static` containers pay this once
- DynamoDB Local (standalone JAR) was rejected: it doesn't emulate S3 or SSM, requiring multiple emulators
- Real AWS dev account was rejected: risk of forgotten resources incurring cost; credentials in CI add operational burden
- Pin LocalStack image version at project start and update deliberately — breaking changes between LocalStack versions are real
