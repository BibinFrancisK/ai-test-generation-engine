# Two IAM roles: task execution role (ECR + CloudWatch) and task role (S3 + DynamoDB + SSM)

data "aws_iam_policy_document" "ecs_tasks_assume_role" {
  statement {
    effect  = "Allow"
    actions = ["sts:AssumeRole"]

    principals {
      type        = "Service"
      identifiers = ["ecs-tasks.amazonaws.com"]
    }
  }
}

resource "aws_iam_role" "testgen_ecs_execution_role" {
  name               = "testgen-ecs-execution-role"
  assume_role_policy = data.aws_iam_policy_document.ecs_tasks_assume_role.json

  tags = local.common_tags
}

resource "aws_iam_role_policy_attachment" "testgen_ecs_execution_role_managed" {
  role       = aws_iam_role.testgen_ecs_execution_role.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AmazonECSTaskExecutionRolePolicy"
}

data "aws_iam_policy_document" "testgen_execution_ssm_policy" {
  statement {
    sid       = "SsmSecretsResolution"
    effect    = "Allow"
    actions   = ["ssm:GetParameters"]
    resources = ["${local.ssm_parameter_arn_prefix}/*"]
  }

  statement {
    sid       = "SsmSecretsDecryption"
    effect    = "Allow"
    actions   = ["kms:Decrypt"]
    resources = [data.aws_kms_alias.ssm.target_key_arn]
  }
}

resource "aws_iam_policy" "testgen_execution_ssm_policy" {
  name   = "testgen-execution-ssm-policy"
  policy = data.aws_iam_policy_document.testgen_execution_ssm_policy.json
}

resource "aws_iam_role_policy_attachment" "testgen_ecs_execution_role_ssm" {
  role       = aws_iam_role.testgen_ecs_execution_role.name
  policy_arn = aws_iam_policy.testgen_execution_ssm_policy.arn
}

resource "aws_iam_role" "testgen_ecs_task_role" {
  name               = "testgen-ecs-task-role"
  assume_role_policy = data.aws_iam_policy_document.ecs_tasks_assume_role.json

  tags = local.common_tags
}

data "aws_iam_policy_document" "testgen_task_policy" {
  statement {
    sid       = "ArtifactsBucketAccess"
    effect    = "Allow"
    actions   = ["s3:PutObject", "s3:GetObject"]
    resources = ["${aws_s3_bucket.artifacts.arn}/*"]
  }

  statement {
    sid       = "TestRunsTableAccess"
    effect    = "Allow"
    actions   = ["dynamodb:PutItem", "dynamodb:GetItem", "dynamodb:Query"]
    resources = [aws_dynamodb_table.test_runs.arn]
  }

  statement {
    sid       = "SsmParameterAccess"
    effect    = "Allow"
    actions   = ["ssm:GetParameter", "ssm:GetParametersByPath"]
    resources = ["${local.ssm_parameter_arn_prefix}", "${local.ssm_parameter_arn_prefix}/*"]
  }

  statement {
    sid       = "SsmParameterDecryption"
    effect    = "Allow"
    actions   = ["kms:Decrypt"]
    resources = [data.aws_kms_alias.ssm.target_key_arn]
  }
}

resource "aws_iam_policy" "testgen_task_policy" {
  name   = "testgen-task-policy"
  policy = data.aws_iam_policy_document.testgen_task_policy.json
}

resource "aws_iam_role_policy_attachment" "testgen_ecs_task_role_policy" {
  role       = aws_iam_role.testgen_ecs_task_role.name
  policy_arn = aws_iam_policy.testgen_task_policy.arn
}
