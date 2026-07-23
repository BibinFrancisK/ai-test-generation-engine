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
