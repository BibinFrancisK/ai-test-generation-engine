output "test_runs_table_name" {
  description = "DynamoDB table name for test run metadata"
  value       = aws_dynamodb_table.test_runs.name
}

output "project_conventions_table_name" {
  description = "DynamoDB table name for per-repo project conventions"
  value       = aws_dynamodb_table.project_conventions.name
}
