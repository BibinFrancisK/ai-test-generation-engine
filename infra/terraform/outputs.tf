output "test_runs_table_name" {
  description = "DynamoDB table name for test run metadata"
  value       = aws_dynamodb_table.test_runs.name
}

output "project_conventions_table_name" {
  description = "DynamoDB table name for per-repo project conventions"
  value       = aws_dynamodb_table.project_conventions.name
}

output "s3_artifacts_bucket_name" {
  description = "S3 bucket name for test artifact storage"
  value       = aws_s3_bucket.artifacts.bucket
}

output "ecs_task_execution_role_arn" {
  description = "IAM role ARN used by ECS to pull images from ECR and write logs to CloudWatch"
  value       = aws_iam_role.testgen_ecs_execution_role.arn
}

output "ecs_task_role_arn" {
  description = "IAM role ARN used by the application at runtime for S3/DynamoDB/SSM access"
  value       = aws_iam_role.testgen_ecs_task_role.arn
}

output "alb_dns_name" {
  description = "Public DNS name of the ALB fronting the ECS service"
  value       = aws_lb.testgen.dns_name
}

output "ecr_repository_url" {
  description = "ECR repository URL to push the application image to"
  value       = aws_ecr_repository.app.repository_url
}

output "ecs_cluster_name" {
  description = "ECS cluster name"
  value       = aws_ecs_cluster.testgen.name
}

output "ecs_service_name" {
  description = "ECS service name"
  value       = aws_ecs_service.testgen.name
}
