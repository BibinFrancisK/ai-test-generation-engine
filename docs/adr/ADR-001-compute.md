# ADR-001 — Compute: ECS Fargate + ALB

## Context

Need a serverless container platform for the test generation service. Key criteria:
- Full IaC teardown in one command (`terraform destroy`) — no orphaned billable resources after demos
- No idle cost when no traffic is flowing
- Real-company technology that demonstrates platform engineering depth for the portfolio
- Least-privilege IAM separation between infrastructure duties and application duties

Candidates evaluated: AWS App Runner, ECS Fargate + ALB, AWS Lambda.

## Decision

**ECS Fargate + ALB.** One ECS cluster, one task definition, one Fargate service behind an Application Load Balancer.

Two IAM roles with strict separation:
- **Task execution role** — ECR image pull and CloudWatch Logs write only
- **Task role** — `s3:PutObject`/`s3:GetObject` on the artifact bucket, `dynamodb:PutItem`/`GetItem`/`UpdateItem` on the two named tables, `ssm:GetParameter` on the specific secret paths — nothing else

ALB handles HTTPS termination; the container speaks plain HTTP on port 8080.

## Consequences

- Requires ECR for image storage (free tier: 500 MB/month — keep image lean with a distroless base)
- ALB incurs ~$16/month while running — **run `terraform destroy -auto-approve` immediately after every demo session**
- App Runner was rejected: it abstracts away the networking and IAM layer that makes this a meaningful portfolio artefact
- Lambda was rejected: cold start latency is unacceptable for webhook processing; LangChain4j dependencies make the package too large for Lambda without custom layers
- Fargate Spot can reduce compute cost ~70% for non-critical workloads if needed
