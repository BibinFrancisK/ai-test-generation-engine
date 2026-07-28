# Cost Estimate

Real numbers grounded in the actual resources provisioned by `infra/terraform/`, not generic AWS pricing-page figures. All prices are `us-east-1`, on-demand, approximate — AWS pricing changes over time; treat this as directionally accurate, not a bill.

## The one cost that doesn't care whether you're using it

| Resource | Config | Cost |
|---|---|---|
| **ALB** (`alb.tf`) | 1 Application Load Balancer, 1 target group, HTTP listener | ~$0.0225/hour fixed **+ LCU usage** ≈ **~$16/month if left running continuously** |

The ALB bills by the hour whether or not a single request ever hits it — it's the one resource in this stack that doesn't scale to zero. This is the entire reason `terraform destroy -auto-approve` is the last step of every demo session, not an afterthought.

## Everything else, while running

| Resource | Config (`infra/terraform/`) | Approx. cost while active |
|---|---|---|
| **ECS Fargate** (`ecs.tf`) | 1 task, 0.5 vCPU / 1 GB, `desired_count = 1` | ~$0.025/hour (~$18/month if run 24/7 — not the intended usage pattern) |
| **DynamoDB** (`dynamodb.tf`) | 2 tables (`test-runs`, `project-conventions`), `PAY_PER_REQUEST` | Within AWS free tier at portfolio-demo request volume |
| **S3** (`s3.tf`) | 1 bucket, versioning + AES-256 SSE, storing generated `.java` test files (KBs each) | Within free tier (5 GB) — a full demo run stores a handful of small files |
| **ECR** (`ecr.tf`) | 1 repository, lifecycle policy caps history at the last 5 images | Well under the 500 MB/month free tier |
| **SSM Parameter Store** (`iam.tf`) | 4 `SecureString` parameters (`LLM_API_KEY`, `GITHUB_APP_PRIVATE_KEY`, `GITHUB_WEBHOOK_SECRET`, `GITHUB_APP_ID`), decrypted via the AWS-managed `alias/aws/ssm` KMS key | Standard-tier parameters are free; KMS requests are free up to 20,000/month |
| **CloudWatch Logs** (`ecs.tf`) | 1 log group, 7-day retention | Within free tier at this log volume |
| **IAM** (`iam.tf`) | 2 roles (task execution, task) | Free — IAM has no charge |

At demo-session scale (a couple of hours, a handful of webhook-triggered runs), the ECS + ALB combination runs to roughly **$0.05–0.06/hour** — a 2-hour recording session costs on the order of **10–15 cents** in AWS compute. DynamoDB, S3, ECR, SSM, and CloudWatch stay inside the AWS free tier the entire time.

## The one number that isn't AWS

| Item | Guard |
|---|---|
| **Anthropic API** (LLM calls) | `max_tokens=1500` enforced per call (`Constants.HARD_MAX_TOKENS`, belt-and-suspenders with `application.yml`); **$10/month hard spend cap** set directly in the Anthropic console |

## Worst case: forgetting to tear down

If `terraform destroy` never runs, the fixed costs (ALB + a continuously-running Fargate task) compound to roughly **$30–35/month** — small in absolute terms, but entirely avoidable. The one-command teardown from `infra/terraform/`:

```bash
terraform destroy -auto-approve
```

This was run and verified at the end of the security-hardening pass — 23/23 resources destroyed, spot-checked via the AWS CLI across ECS, ALB, DynamoDB, S3, and ECR for orphans. Zero billable resources remain between demo sessions.
