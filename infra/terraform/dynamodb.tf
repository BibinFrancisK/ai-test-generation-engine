resource "aws_dynamodb_table" "test_runs" {
  name         = "test-runs"
  billing_mode = "PAY_PER_REQUEST"
  hash_key     = "testRunId"
  range_key    = "repositoryId"

  attribute {
    name = "testRunId"
    type = "S"
  }

  attribute {
    name = "repositoryId"
    type = "S"
  }

  tags = {
    Environment = var.environment
    Project     = "ai-test-generation-engine"
    ManagedBy   = "terraform"
  }
}

resource "aws_dynamodb_table" "project_conventions" {
  name         = "project-conventions"
  billing_mode = "PAY_PER_REQUEST"
  hash_key     = "repositoryId"

  attribute {
    name = "repositoryId"
    type = "S"
  }

  tags = {
    Environment = var.environment
    Project     = "ai-test-generation-engine"
    ManagedBy   = "terraform"
  }
}
